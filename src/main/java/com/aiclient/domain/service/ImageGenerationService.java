package com.aiclient.domain.service;

import com.aiclient.domain.model.GeneratedImage;
import com.aiclient.domain.port.input.ImageGenerationUseCase;
import com.aiclient.domain.port.output.ImageAIPort;
import com.aiclient.domain.port.output.ImagePersistencePort;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ImageGenerationService implements ImageGenerationUseCase {

    private final ImageAIPort imageAIPort;
    private final ImagePersistencePort imagePersistencePort;

    public ImageGenerationService(ImageAIPort imageAIPort, ImagePersistencePort imagePersistencePort) {
        this.imageAIPort = Objects.requireNonNull(imageAIPort, "ImageAIPort cannot be null");
        this.imagePersistencePort = Objects.requireNonNull(imagePersistencePort, "ImagePersistencePort cannot be null");
    }

    @Override
    public GeneratedImage generateImage(String prompt, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        Path generatedFilePath = imageAIPort.generateImage(prompt, modelId);

        String imageId = UUID.randomUUID().toString();
        LocalDateTime generatedAt = LocalDateTime.now();

        GeneratedImage image = new GeneratedImage(
                imageId,
                prompt,
                modelId,
                generatedFilePath,
                generatedAt
        );

        return imagePersistencePort.saveImage(image);
    }

    @Override
    public List<GeneratedImage> getAllImages() {
        return imagePersistencePort.findAllImages();
    }

    @Override
    public void deleteImage(String imageId) {
        Objects.requireNonNull(imageId, "Image ID cannot be null");

        imagePersistencePort.deleteImage(imageId);
    }

    @Override
    public GeneratedImage getImage(String imageId) {
        Objects.requireNonNull(imageId, "Image ID cannot be null");

        return imagePersistencePort.findImageById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
    }
}
