package com.aiclient.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a single message in a chat session.
 */
public class ChatMessage {

    private final String id;
    private final String sessionId;
    private final MessageRole role;
    private final String content;
    private final LocalDateTime timestamp;

    public ChatMessage(String id, String sessionId, MessageRole role, String content, LocalDateTime timestamp) {
        this.id = Objects.requireNonNull(id, "Message ID cannot be null");
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
        this.role = Objects.requireNonNull(role, "Message role cannot be null");
        this.content = Objects.requireNonNull(content, "Message content cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", role=" + role +
                ", timestamp=" + timestamp +
                '}';
    }
}
