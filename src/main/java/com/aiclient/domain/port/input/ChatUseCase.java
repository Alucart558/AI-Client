package com.aiclient.domain.port.input;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;

import java.util.List;

/**
 * Driving port for chat functionality.
 * Defines what the application offers for conversational AI interactions.
 */
public interface ChatUseCase {

    /**
     * Send a message to the AI and receive a response.
     *
     * @param sessionId Unique identifier for the chat session
     * @param userMessage User's input message
     * @return AI's response message
     */
    ChatMessage sendMessage(String sessionId, String userMessage);

    /**
     * Create a new chat session.
     *
     * @param modelId ID of the AI model to use
     * @return Newly created chat session
     */
    ChatSession createSession(String modelId);

    /**
     * Retrieve chat history for a specific session.
     *
     * @param sessionId Session identifier
     * @return List of chat messages in chronological order
     */
    List<ChatMessage> getSessionHistory(String sessionId);

    /**
     * List all chat sessions for the user.
     *
     * @return List of all chat sessions
     */
    List<ChatSession> getAllSessions();

    /**
     * Permanently delete a chat session and all its messages.
     *
     * @param sessionId Session identifier
     */
    void deleteSession(String sessionId);
}
