package com.aiclient.domain.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an AI-generated image.
 */
public class GeneratedImage {

    private final String id;
    private final String prompt;
    private final String modelId;
    private final Path filePath;
    private final LocalDateTime generatedAt;

    public GeneratedImage(String id, String prompt, String modelId, Path filePath, LocalDateTime generatedAt) {
        this.id = Objects.requireNonNull(id, "Image ID cannot be null");
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.modelId = Objects.requireNonNull(modelId, "Model ID cannot be null");
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "Generation time cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getModelId() {
        return modelId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratedImage that = (GeneratedImage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GeneratedImage{" +
                "id='" + id + '\'' +
                ", modelId='" + modelId + '\'' +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
