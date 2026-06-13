package com.aiclient.adapter.output.persistence;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.model.MessageRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SQLiteChatPersistenceAdapterTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private SQLiteChatPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        String dbPath = tempDir.resolve("test.db").toString();
        databaseManager = new DatabaseManager(dbPath);
        adapter = new SQLiteChatPersistenceAdapter(databaseManager);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    void shouldCreateAdapter() {
        assertThat(adapter).isNotNull();
    }

    @Test
    void shouldRejectNullDatabaseManager() {
        assertThatThrownBy(() -> new SQLiteChatPersistenceAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DatabaseManager cannot be null");
    }

    @Test
    void shouldSaveSession() {
        ChatSession session = new ChatSession("session-1", "llama2", LocalDateTime.now());

        ChatSession saved = adapter.saveSession(session);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("session-1");
        assertThat(saved.getModelId()).isEqualTo("llama2");
    }

    @Test
    void shouldRejectNullSessionOnSave() {
        assertThatThrownBy(() -> adapter.saveSession(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session cannot be null");
    }

    @Test
    void shouldFindSessionById() {
        ChatSession session = new ChatSession("session-1", "llama2", LocalDateTime.now());
        adapter.saveSession(session);

        Optional<ChatSession> found = adapter.findSessionById("session-1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("session-1");
        assertThat(found.get().getModelId()).isEqualTo("llama2");
    }

    @Test
    void shouldReturnEmptyWhenSessionNotFound() {
        Optional<ChatSession> found = adapter.findSessionById("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldRejectNullSessionIdOnFind() {
        assertThatThrownBy(() -> adapter.findSessionById(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldFindAllSessions() {
        ChatSession session1 = new ChatSession("session-1", "llama2", LocalDateTime.now());
        ChatSession session2 = new ChatSession("session-2", "gpt-3", LocalDateTime.now().plusMinutes(1));

        adapter.saveSession(session1);
        adapter.saveSession(session2);

        List<ChatSession> sessions = adapter.findAllSessions();

        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(ChatSession::getId)
                .containsExactlyInAnyOrder("session-1", "session-2");
    }

    @Test
    void shouldReturnEmptyListWhenNoSessions() {
        List<ChatSession> sessions = adapter.findAllSessions();
        assertThat(sessions).isEmpty();
    }

    @Test
    void shouldDeleteSession() {
        ChatSession session = new ChatSession("session-1", "llama2", LocalDateTime.now());
        adapter.saveSession(session);

        adapter.deleteSession("session-1");

        Optional<ChatSession> found = adapter.findSessionById("session-1");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldHandleDeleteNonexistentSession() {
        adapter.deleteSession("nonexistent");
    }

    @Test
    void shouldRejectNullSessionIdOnDelete() {
        assertThatThrownBy(() -> adapter.deleteSession(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldSaveMessage() {
        ChatMessage message = new ChatMessage(
                "msg-1",
                "session-1",
                MessageRole.USER,
                "Hello",
                LocalDateTime.now()
        );

        ChatMessage saved = adapter.saveMessage(message);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("msg-1");
        assertThat(saved.getContent()).isEqualTo("Hello");
    }

    @Test
    void shouldRejectNullMessageOnSave() {
        assertThatThrownBy(() -> adapter.saveMessage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Message cannot be null");
    }

    @Test
    void shouldFindMessagesBySessionId() {
        LocalDateTime now = LocalDateTime.now();

        ChatMessage msg1 = new ChatMessage("msg-1", "session-1", MessageRole.USER, "Hello", now);
        ChatMessage msg2 = new ChatMessage("msg-2", "session-1", MessageRole.ASSISTANT, "Hi", now.plusSeconds(1));

        adapter.saveMessage(msg1);
        adapter.saveMessage(msg2);

        List<ChatMessage> messages = adapter.findMessagesBySessionId("session-1");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello");
        assertThat(messages.get(1).getContent()).isEqualTo("Hi");
    }

    @Test
    void shouldReturnEmptyListWhenNoMessages() {
        List<ChatMessage> messages = adapter.findMessagesBySessionId("nonexistent");
        assertThat(messages).isEmpty();
    }

    @Test
    void shouldRejectNullSessionIdOnFindMessages() {
        assertThatThrownBy(() -> adapter.findMessagesBySessionId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldDeleteMessagesBySessionId() {
        ChatMessage msg1 = new ChatMessage("msg-1", "session-1", MessageRole.USER, "Hello", LocalDateTime.now());
        ChatMessage msg2 = new ChatMessage("msg-2", "session-1", MessageRole.ASSISTANT, "Hi", LocalDateTime.now());

        adapter.saveMessage(msg1);
        adapter.saveMessage(msg2);

        adapter.deleteMessagesBySessionId("session-1");

        List<ChatMessage> messages = adapter.findMessagesBySessionId("session-1");
        assertThat(messages).isEmpty();
    }

    @Test
    void shouldRejectNullSessionIdOnDeleteMessages() {
        assertThatThrownBy(() -> adapter.deleteMessagesBySessionId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Session ID cannot be null");
    }

    @Test
    void shouldUpdateSessionOnReplaceInsert() {
        ChatSession session = new ChatSession("session-1", "llama2", LocalDateTime.now());
        session.setTitle("Original Title");

        adapter.saveSession(session);

        session.setTitle("Updated Title");
        adapter.saveSession(session);

        Optional<ChatSession> found = adapter.findSessionById("session-1");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void shouldPreserveMessageRole() {
        ChatMessage userMsg = new ChatMessage("msg-1", "s-1", MessageRole.USER, "Test", LocalDateTime.now());
        ChatMessage assistantMsg = new ChatMessage("msg-2", "s-1", MessageRole.ASSISTANT, "Test", LocalDateTime.now());
        ChatMessage systemMsg = new ChatMessage("msg-3", "s-1", MessageRole.SYSTEM, "Test", LocalDateTime.now());

        adapter.saveMessage(userMsg);
        adapter.saveMessage(assistantMsg);
        adapter.saveMessage(systemMsg);

        List<ChatMessage> messages = adapter.findMessagesBySessionId("s-1");

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(2).getRole()).isEqualTo(MessageRole.SYSTEM);
    }
}
