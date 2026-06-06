package com.aiclient.domain.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Domain entity representing an AI model available locally.
 */
public class AIModel {

    private final String id;
    private final String name;
    private final ModelType type;
    private final Path filePath;
    private final long fileSizeBytes;

    public AIModel(String id, String name, ModelType type, Path filePath, long fileSizeBytes) {
        this.id = Objects.requireNonNull(id, "Model ID cannot be null");
        this.name = Objects.requireNonNull(name, "Model name cannot be null");
        this.type = Objects.requireNonNull(type, "Model type cannot be null");
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ModelType getType() {
        return type;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIModel aiModel = (AIModel) o;
        return Objects.equals(id, aiModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AIModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
