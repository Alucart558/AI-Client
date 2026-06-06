package com.aiclient.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a chat conversation session.
 */
public class ChatSession {

    private final String id;
    private final String modelId;
    private final LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private String title;

    public ChatSession(String id, String modelId, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "Session ID cannot be null");
        this.modelId = Objects.requireNonNull(modelId, "Model ID cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time cannot be null");
        this.lastMessageAt = createdAt;
        this.title = "New Chat";
    }

    public String getId() {
        return id;
    }

    public String getModelId() {
        return modelId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = Objects.requireNonNull(lastMessageAt);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatSession that = (ChatSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "id='" + id + '\'' +
                ", modelId='" + modelId + '\'' +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
