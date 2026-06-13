package com.aiclient.adapter.output.persistence;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.model.MessageRole;
import com.aiclient.domain.port.output.ChatPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite implementation of ChatPersistencePort.
 * Provides persistent storage for chat sessions and messages.
 */
public class SQLiteChatPersistenceAdapter implements ChatPersistencePort {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteChatPersistenceAdapter.class);

    private final DatabaseManager databaseManager;

    public SQLiteChatPersistenceAdapter(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager cannot be null");
        this.databaseManager.initialize();
    }

    @Override
    public ChatSession saveSession(ChatSession session) {
        Objects.requireNonNull(session, "Session cannot be null");

        String sql = "INSERT OR REPLACE INTO chat_sessions (id, model_id, title, created_at, last_message_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, session.getId());
                stmt.setString(2, session.getModelId());
                stmt.setString(3, session.getTitle());
                stmt.setString(4, session.getCreatedAt().toString());
                stmt.setString(5, session.getLastMessageAt().toString());

                stmt.executeUpdate();
                logger.debug("Saved chat session: {}", session.getId());
                return session;
            }
        } catch (SQLException e) {
            logger.error("Failed to save session {}: {}", session.getId(), e.getMessage());
            throw new RuntimeException("Failed to save session: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ChatSession> findSessionById(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        String sql = "SELECT id, model_id, title, created_at, last_message_at FROM chat_sessions WHERE id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ChatSession session = mapResultSetToSession(rs);
                        logger.debug("Found session: {}", sessionId);
                        return Optional.of(session);
                    }
                }

                logger.debug("Session not found: {}", sessionId);
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Failed to find session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to find session: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatSession> findAllSessions() {
        String sql = "SELECT id, model_id, title, created_at, last_message_at FROM chat_sessions " +
                "ORDER BY last_message_at DESC";

        List<ChatSession> sessions = new ArrayList<>();

        try {
            Connection conn = databaseManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }

                logger.debug("Found {} sessions", sessions.size());
                return sessions;
            }
        } catch (SQLException e) {
            logger.error("Failed to find all sessions: {}", e.getMessage());
            throw new RuntimeException("Failed to find sessions: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        String sql = "DELETE FROM chat_sessions WHERE id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    logger.info("Deleted session: {}", sessionId);
                } else {
                    logger.warn("Session not found for deletion: {}", sessionId);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to delete session: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");

        String sql = "INSERT OR REPLACE INTO chat_messages (id, session_id, role, content, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, message.getId());
                stmt.setString(2, message.getSessionId());
                stmt.setString(3, message.getRole().name());
                stmt.setString(4, message.getContent());
                stmt.setString(5, message.getTimestamp().toString());

                stmt.executeUpdate();
                logger.debug("Saved message: {}", message.getId());
                return message;
            }
        } catch (SQLException e) {
            logger.error("Failed to save message {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Failed to save message: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatMessage> findMessagesBySessionId(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        String sql = "SELECT id, session_id, role, content, timestamp FROM chat_messages " +
                "WHERE session_id = ? ORDER BY timestamp ASC";

        List<ChatMessage> messages = new ArrayList<>();

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        messages.add(mapResultSetToMessage(rs));
                    }
                }

                logger.debug("Found {} messages for session {}", messages.size(), sessionId);
                return messages;
            }
        } catch (SQLException e) {
            logger.error("Failed to find messages for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to find messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessagesBySessionId(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        String sql = "DELETE FROM chat_messages WHERE session_id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                int deleted = stmt.executeUpdate();

                logger.debug("Deleted {} messages for session {}", deleted, sessionId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete messages for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to delete messages: " + e.getMessage(), e);
        }
    }

    private ChatSession mapResultSetToSession(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String modelId = rs.getString("model_id");
        String title = rs.getString("title");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime lastMessageAt = LocalDateTime.parse(rs.getString("last_message_at"));

        ChatSession session = new ChatSession(id, modelId, createdAt);
        session.setTitle(title);
        session.setLastMessageAt(lastMessageAt);

        return session;
    }

    private ChatMessage mapResultSetToMessage(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String sessionId = rs.getString("session_id");
        MessageRole role = MessageRole.valueOf(rs.getString("role"));
        String content = rs.getString("content");
        LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));

        return new ChatMessage(id, sessionId, role, content, timestamp);
    }
}
