package com.aiclient.domain.service;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.model.MessageRole;
import com.aiclient.domain.port.output.ChatPersistencePort;
import com.aiclient.domain.port.output.TextAIPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private TextAIPort textAIPort;

    @Mock
    private ChatPersistencePort chatPersistencePort;

    @InjectMocks
    private ChatService chatService;

    @Test
    void shouldRejectNullTextAIPort() {
        assertThatThrownBy(() -> new ChatService(null, chatPersistencePort))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("TextAIPort cannot be null");
    }

    @Test
    void shouldRejectNullChatPersistencePort() {
        assertThatThrownBy(() -> new ChatService(textAIPort, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ChatPersistencePort cannot be null");
    }

    @Test
    void shouldCreateNewSession() {
        when(textAIPort.isModelAvailable("llama2")).thenReturn(true);
        when(chatPersistencePort.saveSession(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession session = chatService.createSession("llama2");

        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotNull();
        assertThat(session.getModelId()).isEqualTo("llama2");
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getTitle()).contains("llama2");

        verify(textAIPort).isModelAvailable("llama2");
        verify(chatPersistencePort).saveSession(any(ChatSession.class));
    }

    @Test
    void shouldRejectNullModelIdOnCreateSession() {
        assertThatThrownBy(() -> chatService.createSession(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldRejectUnavailableModelOnCreateSession() {
        when(textAIPort.isModelAvailable("invalid-model")).thenReturn(false);

        assertThatThrownBy(() -> chatService.createSession("invalid-model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model 'invalid-model' is not available in Ollama");

        verify(textAIPort).isModelAvailable("invalid-model");
    }

    @Test
    void shouldSendMessageAndReceiveResponse() {
        String sessionId = "session-1";
        String userMessage = "Hello, AI!";
        String aiResponse = "Hello, human!";

        ChatSession session = new ChatSession(sessionId, "llama2", LocalDateTime.now());

        when(chatPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(session));
        when(chatPersistencePort.findMessagesBySessionId(sessionId)).thenReturn(List.of());
        when(textAIPort.generateResponseWithContext(anyString(), anyString(), eq("llama2")))
                .thenReturn(aiResponse);
        when(chatPersistencePort.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPersistencePort.saveSession(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage response = chatService.sendMessage(sessionId, userMessage);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(response.getContent()).isEqualTo(aiResponse);
        assertThat(response.getSessionId()).isEqualTo(sessionId);

        verify(chatPersistencePort, times(2)).saveMessage(any(ChatMessage.class));
        verify(textAIPort).generateResponseWithContext(eq(userMessage), anyString(), eq("llama2"));
        verify(chatPersistencePort).saveSession(session);
    }

    @Test
    void shouldRejectNullSessionIdOnSendMessage() {
        assertThatThrownBy(() -> chatService.sendMessage(null, "Hello"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldRejectNullUserMessageOnSendMessage() {
        assertThatThrownBy(() -> chatService.sendMessage("session-1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User message cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenSessionNotFoundOnSendMessage() {
        when(chatPersistencePort.findSessionById("nonexistent")).thenReturn(Optional.empty());
        when(chatPersistencePort.findMessagesBySessionId("nonexistent")).thenReturn(List.of());
        when(chatPersistencePort.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> chatService.sendMessage("nonexistent", "Hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void shouldIncludeConversationHistoryInAIRequest() {
        String sessionId = "session-1";
        String userMessage = "What's the weather?";
        ChatSession session = new ChatSession(sessionId, "llama2", LocalDateTime.now());

        ChatMessage previousUserMsg = new ChatMessage(
                "msg-1",
                sessionId,
                MessageRole.USER,
                "Hello",
                LocalDateTime.now().minusMinutes(5)
        );
        ChatMessage previousAiMsg = new ChatMessage(
                "msg-2",
                sessionId,
                MessageRole.ASSISTANT,
                "Hi there!",
                LocalDateTime.now().minusMinutes(4)
        );

        when(chatPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(session));
        when(chatPersistencePort.findMessagesBySessionId(sessionId))
                .thenReturn(Arrays.asList(previousUserMsg, previousAiMsg));
        when(textAIPort.generateResponseWithContext(anyString(), anyString(), anyString()))
                .thenReturn("It's sunny!");
        when(chatPersistencePort.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPersistencePort.saveSession(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.sendMessage(sessionId, userMessage);

        verify(textAIPort).generateResponseWithContext(
                eq(userMessage),
                eq("User: Hello\nAssistant: Hi there!\n"),
                eq("llama2")
        );
    }

    @Test
    void shouldGetSessionHistory() {
        String sessionId = "session-1";
        ChatMessage msg1 = new ChatMessage("msg-1", sessionId, MessageRole.USER, "Hello", LocalDateTime.now());
        ChatMessage msg2 = new ChatMessage("msg-2", sessionId, MessageRole.ASSISTANT, "Hi", LocalDateTime.now());

        when(chatPersistencePort.findMessagesBySessionId(sessionId))
                .thenReturn(Arrays.asList(msg1, msg2));

        List<ChatMessage> history = chatService.getSessionHistory(sessionId);

        assertThat(history).hasSize(2);
        assertThat(history).containsExactly(msg1, msg2);

        verify(chatPersistencePort).findMessagesBySessionId(sessionId);
    }

    @Test
    void shouldRejectNullSessionIdOnGetSessionHistory() {
        assertThatThrownBy(() -> chatService.getSessionHistory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldGetAllSessions() {
        ChatSession session1 = new ChatSession("session-1", "llama2", LocalDateTime.now());
        ChatSession session2 = new ChatSession("session-2", "gpt-3", LocalDateTime.now());

        when(chatPersistencePort.findAllSessions())
                .thenReturn(Arrays.asList(session1, session2));

        List<ChatSession> sessions = chatService.getAllSessions();

        assertThat(sessions).hasSize(2);
        assertThat(sessions).containsExactly(session1, session2);

        verify(chatPersistencePort).findAllSessions();
    }

    @Test
    void shouldDeleteSession() {
        String sessionId = "session-1";

        chatService.deleteSession(sessionId);

        verify(chatPersistencePort).deleteMessagesBySessionId(sessionId);
        verify(chatPersistencePort).deleteSession(sessionId);
    }

    @Test
    void shouldRejectNullSessionIdOnDeleteSession() {
        assertThatThrownBy(() -> chatService.deleteSession(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldHandleEmptyConversationHistory() {
        String sessionId = "session-1";
        ChatSession session = new ChatSession(sessionId, "llama2", LocalDateTime.now());

        when(chatPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(session));
        when(chatPersistencePort.findMessagesBySessionId(sessionId)).thenReturn(List.of());
        when(textAIPort.generateResponseWithContext(anyString(), anyString(), anyString()))
                .thenReturn("Response");
        when(chatPersistencePort.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPersistencePort.saveSession(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.sendMessage(sessionId, "First message");

        verify(textAIPort).generateResponseWithContext(
                eq("First message"),
                eq(""),
                eq("llama2")
        );
    }
}
