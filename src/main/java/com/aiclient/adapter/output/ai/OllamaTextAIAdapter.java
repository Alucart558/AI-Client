package com.aiclient.adapter.output.ai;

import com.aiclient.domain.port.output.TextAIPort;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * Adapter implementing TextAIPort using Ollama via LangChain4j.
 * Provides text generation capabilities using locally running Ollama models.
 */
public class OllamaTextAIAdapter implements TextAIPort {

    private static final Logger logger = LoggerFactory.getLogger(OllamaTextAIAdapter.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final String baseUrl;
    private final Duration timeout;

    public OllamaTextAIAdapter() {
        this(DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
    }

    public OllamaTextAIAdapter(String baseUrl, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
    }

    @Override
    public String generateResponse(String prompt, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        logger.debug("Generating response with model: {}", modelId);

        try {
            ChatLanguageModel model = createChatModel(modelId);
            String response = model.generate(prompt);
            logger.debug("Response generated successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error generating response with model {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateResponseWithContext(String prompt, String conversationHistory, String modelId) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(conversationHistory, "Conversation history cannot be null");
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        logger.debug("Generating response with context using model: {}", modelId);

        try {
            ChatLanguageModel model = createChatModel(modelId);
            String fullPrompt = conversationHistory + "\n\n" + prompt;
            String response = model.generate(fullPrompt);
            logger.debug("Response with context generated successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error generating response with context for model {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Failed to generate response with context: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            logger.debug("Checking Ollama availability at: {}", baseUrl);

            ChatLanguageModel testModel = OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .timeout(Duration.ofSeconds(5))
                    .modelName("llama2")
                    .build();

            testModel.generate("test");
            logger.debug("Ollama service is available");
            return true;
        } catch (Exception e) {
            logger.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }

    private ChatLanguageModel createChatModel(String modelId) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .timeout(timeout)
                .modelName(modelId)
                .build();
    }
}
