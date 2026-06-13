package com.aiclient.adapter.output.ai;

import com.aiclient.domain.port.output.ImageAIPort;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter implementing ImageAIPort using Stable Diffusion WebUI API.
 * Communicates with locally running Stable Diffusion instance via HTTP.
 */
public class StableDiffusionImageAdapter implements ImageAIPort {

    private static final Logger logger = LoggerFactory.getLogger(StableDiffusionImageAdapter.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:7860";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final String TXT2IMG_ENDPOINT = "/sdapi/v1/txt2img";
    private static final String OUTPUT_DIR = "generated_images";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Path outputDirectory;

    public StableDiffusionImageAdapter() {
        this(DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
    }

    public StableDiffusionImageAdapter(String baseUrl, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        Objects.requireNonNull(timeout, "Timeout cannot be null");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.gson = new Gson();

        this.outputDirectory = Paths.get(OUTPUT_DIR);
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + e.getMessage(), e);
        }
    }

    @Override
    public Path generateImage(String prompt, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        logger.debug("Generating image with prompt: {}", prompt);

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("steps", 20);
            requestBody.addProperty("width", 512);
            requestBody.addProperty("height", 512);
            requestBody.addProperty("cfg_scale", 7);
            requestBody.addProperty("sampler_name", "Euler");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + TXT2IMG_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Stable Diffusion API returned status: " + response.statusCode());
            }

            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            String base64Image = responseJson.getAsJsonArray("images").get(0).getAsString();

            Path imagePath = saveImage(base64Image);
            logger.debug("Image generated and saved to: {}", imagePath);

            return imagePath;
        } catch (IOException | InterruptedException e) {
            logger.error("Error generating image: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            logger.debug("Checking Stable Diffusion availability at: {}", baseUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/sdapi/v1/sd-models"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean available = response.statusCode() == 200;

            if (available) {
                logger.debug("Stable Diffusion service is available");
            } else {
                logger.warn("Stable Diffusion service returned status: {}", response.statusCode());
            }

            return available;
        } catch (IOException | InterruptedException e) {
            logger.warn("Stable Diffusion service is not available: {}", e.getMessage());
            return false;
        }
    }

    private Path saveImage(String base64Image) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String filename = "image_" + UUID.randomUUID() + ".png";
        Path imagePath = outputDirectory.resolve(filename);
        Files.write(imagePath, imageBytes);
        return imagePath;
    }
}
