package com.aiclient.domain.service;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.model.MessageRole;
import com.aiclient.domain.port.input.ChatUseCase;
import com.aiclient.domain.port.output.ChatPersistencePort;
import com.aiclient.domain.port.output.TextAIPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatService implements ChatUseCase {

    private final TextAIPort textAIPort;
    private final ChatPersistencePort chatPersistencePort;

    public ChatService(TextAIPort textAIPort, ChatPersistencePort chatPersistencePort) {
        this.textAIPort = Objects.requireNonNull(textAIPort, "TextAIPort cannot be null");
        this.chatPersistencePort = Objects.requireNonNull(chatPersistencePort, "ChatPersistencePort cannot be null");
    }

    @Override
    public ChatMessage sendMessage(String sessionId, String userMessage) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");
        Objects.requireNonNull(userMessage, "User message cannot be null");

        String userMessageId = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();

        ChatMessage userChatMessage = new ChatMessage(
                userMessageId,
                sessionId,
                MessageRole.USER,
                userMessage,
                timestamp
        );
        chatPersistencePort.saveMessage(userChatMessage);

        List<ChatMessage> history = chatPersistencePort.findMessagesBySessionId(sessionId);
        String conversationHistory = buildConversationHistory(history);

        ChatSession session = chatPersistencePort.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        String aiResponse = textAIPort.generateResponseWithContext(
                userMessage,
                conversationHistory,
                session.getModelId()
        );

        String assistantMessageId = UUID.randomUUID().toString();
        LocalDateTime assistantTimestamp = LocalDateTime.now();

        ChatMessage assistantMessage = new ChatMessage(
                assistantMessageId,
                sessionId,
                MessageRole.ASSISTANT,
                aiResponse,
                assistantTimestamp
        );

        chatPersistencePort.saveMessage(assistantMessage);

        session.setLastMessageAt(assistantTimestamp);
        chatPersistencePort.saveSession(session);

        return assistantMessage;
    }

    @Override
    public ChatSession createSession(String modelId) {
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ChatSession session = new ChatSession(sessionId, modelId, now);

        return chatPersistencePort.saveSession(session);
    }

    @Override
    public List<ChatMessage> getSessionHistory(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        return chatPersistencePort.findMessagesBySessionId(sessionId);
    }

    @Override
    public List<ChatSession> getAllSessions() {
        return chatPersistencePort.findAllSessions();
    }

    @Override
    public void deleteSession(String sessionId) {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");

        chatPersistencePort.deleteMessagesBySessionId(sessionId);
        chatPersistencePort.deleteSession(sessionId);
    }

    private String buildConversationHistory(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder history = new StringBuilder();
        for (ChatMessage message : messages) {
            String roleName = message.getRole() == MessageRole.USER ? "User" : "Assistant";
            history.append(roleName).append(": ").append(message.getContent()).append("\n");
        }
        return history.toString();
    }
}
