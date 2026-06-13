package com.aiclient.adapter.output.ai;

import com.aiclient.domain.port.output.TextAIPort;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter implementing TextAIPort using Ollama via LangChain4j.
 * Provides text generation capabilities using locally running Ollama models.
 */
public class OllamaTextAIAdapter implements TextAIPort {

    private static final Logger logger = LoggerFactory.getLogger(OllamaTextAIAdapter.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final String baseUrl;
    private final Duration timeout;

    public OllamaTextAIAdapter() {
        this(DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
    }

    public OllamaTextAIAdapter(String baseUrl, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
    }

    @Override
    public String generateResponse(String prompt, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        logger.debug("Generating response with model: {}", modelId);

        try {
            ChatLanguageModel model = createChatModel(modelId);
            String response = model.generate(prompt);
            logger.debug("Response generated successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error generating response with model {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateResponseWithContext(String prompt, String conversationHistory, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(conversationHistory, "Conversation history cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        logger.debug("Generating response with context using model: {}", modelId);

        try {
            ChatLanguageModel model = createChatModel(modelId);
            String fullPrompt = conversationHistory + "\n\n" + prompt;
            String response = model.generate(fullPrompt);
            logger.debug("Response with context generated successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error generating response for model {}: {}", modelId, e.getMessage(), e);

            String userMessage;
            if (e.getMessage() != null && (e.getMessage().contains("connect") || e.getMessage().contains("refused"))) {
                userMessage = "Cannot connect to Ollama service at " + baseUrl + ". Is Ollama running?";
            } else if (e.getMessage() != null && (e.getMessage().contains("system memory") || e.getMessage().contains("out of memory"))) {
                userMessage = "Insufficient system memory to run model '" + modelId + "'. " +
                        "Try a smaller model or free up RAM. Details: " + e.getMessage();
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                userMessage = "Model '" + modelId + "' not found. Please select a valid model.";
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                userMessage = "Request timed out. Model might be too large or system too slow.";
            } else {
                userMessage = "AI generation failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
            }

            throw new RuntimeException(userMessage, e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            logger.debug("Checking Ollama availability at: {}", baseUrl);

            ChatLanguageModel testModel = OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .timeout(Duration.ofSeconds(5))
                    .modelName("llama2")
                    .build();

            testModel.generate("test");
            logger.debug("Ollama service is available");
            return true;
        } catch (Exception e) {
            logger.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isModelAvailable(String modelId) {
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        try {
            logger.debug("Checking if model {} is available via Ollama API", modelId);
            List<String> availableModels = listAvailableModels();
            boolean available = availableModels.contains(modelId);
            logger.debug("Model {} availability: {}", modelId, available);
            return available;
        } catch (Exception e) {
            logger.warn("Failed to check model availability: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listAvailableModels() {
        List<String> models = new ArrayList<>();

        try {
            logger.debug("Fetching available models from Ollama API at: {}", baseUrl);

            URL url = new URL(baseUrl + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.error("Failed to fetch models from Ollama. HTTP response code: {}", responseCode);
                return models;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response: {"models":[{"name":"qwen2.5-coder:latest"}, ...]}
            String json = response.toString();
            Pattern pattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);

            while (matcher.find()) {
                String modelName = matcher.group(1);
                models.add(modelName);
            }

            logger.debug("Found {} available models", models.size());

        } catch (Exception e) {
            logger.error("Failed to fetch models from Ollama: {}", e.getMessage());
        }

        return models;
    }

    private ChatLanguageModel createChatModel(String modelId) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .timeout(timeout)
                .modelName(modelId)
                .build();
    }
}
