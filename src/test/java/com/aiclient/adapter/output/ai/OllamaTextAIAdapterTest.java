package com.aiclient.adapter.output.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaTextAIAdapterTest {

    private OllamaTextAIAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OllamaTextAIAdapter("http://localhost:11434", Duration.ofSeconds(30));
    }

    @Test
    void shouldCreateAdapterWithDefaultSettings() {
        OllamaTextAIAdapter defaultAdapter = new OllamaTextAIAdapter();
        assertThat(defaultAdapter).isNotNull();
    }

    @Test
    void shouldCreateAdapterWithCustomSettings() {
        OllamaTextAIAdapter customAdapter = new OllamaTextAIAdapter(
                "http://custom-host:8080",
                Duration.ofMinutes(2)
        );
        assertThat(customAdapter).isNotNull();
    }

    @Test
    void shouldRejectNullBaseUrl() {
        assertThatThrownBy(() -> new OllamaTextAIAdapter(null, Duration.ofSeconds(30)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Base URL cannot be null");
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThatThrownBy(() -> new OllamaTextAIAdapter("http://localhost:11434", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void shouldRejectNullPrompt() {
        assertThatThrownBy(() -> adapter.generateResponse(null, "llama2"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prompt cannot be null");
    }

    @Test
    void shouldRejectNullModelId() {
        assertThatThrownBy(() -> adapter.generateResponse("Hello", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldRejectNullPromptInContextMethod() {
        assertThatThrownBy(() -> adapter.generateResponseWithContext(null, "history", "llama2"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prompt cannot be null");
    }

    @Test
    void shouldRejectNullHistoryInContextMethod() {
        assertThatThrownBy(() -> adapter.generateResponseWithContext("prompt", null, "llama2"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Conversation history cannot be null");
    }

    @Test
    void shouldRejectNullModelIdInContextMethod() {
        assertThatThrownBy(() -> adapter.generateResponseWithContext("prompt", "history", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldHandleServiceUnavailable() {
        OllamaTextAIAdapter unavailableAdapter = new OllamaTextAIAdapter(
                "http://localhost:65535",
                Duration.ofSeconds(1)
        );
        assertThat(unavailableAdapter.isAvailable()).isFalse();
    }
}
