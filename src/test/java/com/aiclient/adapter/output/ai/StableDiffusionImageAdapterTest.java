package com.aiclient.adapter.output.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StableDiffusionImageAdapterTest {

    private StableDiffusionImageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StableDiffusionImageAdapter("http://localhost:7860", Duration.ofMinutes(2));
    }

    @Test
    void shouldCreateAdapterWithDefaultSettings() {
        StableDiffusionImageAdapter defaultAdapter = new StableDiffusionImageAdapter();
        assertThat(defaultAdapter).isNotNull();
    }

    @Test
    void shouldCreateAdapterWithCustomSettings() {
        StableDiffusionImageAdapter customAdapter = new StableDiffusionImageAdapter(
                "http://custom-host:8080",
                Duration.ofMinutes(5)
        );
        assertThat(customAdapter).isNotNull();
    }

    @Test
    void shouldRejectNullBaseUrl() {
        assertThatThrownBy(() -> new StableDiffusionImageAdapter(null, Duration.ofMinutes(2)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Base URL cannot be null");
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThatThrownBy(() -> new StableDiffusionImageAdapter("http://localhost:7860", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void shouldRejectNullPrompt() {
        assertThatThrownBy(() -> adapter.generateImage(null, "sd-model"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prompt cannot be null");
    }

    @Test
    void shouldRejectNullModelId() {
        assertThatThrownBy(() -> adapter.generateImage("A beautiful landscape", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldHandleServiceUnavailable() {
        StableDiffusionImageAdapter unavailableAdapter = new StableDiffusionImageAdapter(
                "http://localhost:65535",
                Duration.ofSeconds(1)
        );
        assertThat(unavailableAdapter.isAvailable()).isFalse();
    }
}
