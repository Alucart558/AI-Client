package com.aiclient.domain.port.output;

/**
 * Driven port for managing external AI service processes.
 * Abstracts lifecycle management of Ollama and Stable Diffusion processes.
 */
public interface ProcessManagementPort {

    /**
     * Start the text AI service (Ollama).
     *
     * @throws ProcessStartException if the process fails to start
     */
    void startTextAIService();

    /**
     * Start the image generation service (Stable Diffusion).
     *
     * @throws ProcessStartException if the process fails to start
     */
    void startImageAIService();

    /**
     * Stop all running AI services.
     */
    void stopAllServices();

    /**
     * Check if the text AI service is running.
     *
     * @return true if running
     */
    boolean isTextAIServiceRunning();

    /**
     * Check if the image AI service is running.
     *
     * @return true if running
     */
    boolean isImageAIServiceRunning();

    /**
     * Exception thrown when a process fails to start.
     */
    class ProcessStartException extends RuntimeException {
        public ProcessStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
