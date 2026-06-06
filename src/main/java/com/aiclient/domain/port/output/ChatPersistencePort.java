package com.aiclient.domain.port.output;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for chat data persistence.
 * Abstracts database operations for chat sessions and messages.
 */
public interface ChatPersistencePort {

    /**
     * Save a new chat session.
     *
     * @param session Session to save
     * @return Saved session with generated ID
     */
    ChatSession saveSession(ChatSession session);

    /**
     * Retrieve a chat session by ID.
     *
     * @param sessionId Session identifier
     * @return Optional containing the session if found
     */
    Optional<ChatSession> findSessionById(String sessionId);

    /**
     * Get all chat sessions.
     *
     * @return List of all sessions
     */
    List<ChatSession> findAllSessions();

    /**
     * Delete a chat session permanently.
     *
     * @param sessionId Session identifier
     */
    void deleteSession(String sessionId);

    /**
     * Save a chat message to a session.
     *
     * @param message Message to save
     * @return Saved message with generated ID
     */
    ChatMessage saveMessage(ChatMessage message);

    /**
     * Get all messages for a specific session.
     *
     * @param sessionId Session identifier
     * @return List of messages in chronological order
     */
    List<ChatMessage> findMessagesBySessionId(String sessionId);

    /**
     * Delete all messages for a session.
     *
     * @param sessionId Session identifier
     */
    void deleteMessagesBySessionId(String sessionId);
}
