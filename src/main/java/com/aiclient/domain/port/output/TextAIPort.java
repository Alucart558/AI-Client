package com.aiclient.domain.port.output;

/**
 * Driven port for text-based AI interactions.
 * Abstracts the underlying LLM implementation (Ollama via LangChain4j).
 */
public interface TextAIPort {

    /**
     * Send a prompt to the text AI model and receive a response.
     *
     * @param prompt User's input text
     * @param modelId ID of the model to use
     * @return AI-generated response
     */
    String generateResponse(String prompt, String modelId);

    /**
     * Send a prompt with conversation context.
     *
     * @param prompt Current user message
     * @param conversationHistory Previous messages in the conversation
     * @param modelId ID of the model to use
     * @return AI-generated response
     */
    String generateResponseWithContext(String prompt, String conversationHistory, String modelId);

    /**
     * Check if the text AI service is available and responsive.
     *
     * @return true if service is healthy
     */
    boolean isAvailable();

    /**
     * Check if a specific model is available and can be used for generation.
     *
     * @param modelId ID of the model to check
     * @return true if the model is available
     */
    boolean isModelAvailable(String modelId);

    /**
     * List all available models from the AI service.
     *
     * @return List of available model names
     */
    java.util.List<String> listAvailableModels();
}
