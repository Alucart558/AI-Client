package com.aiclient.domain.port.output;

import java.nio.file.Path;

/**
 * Driven port for image generation AI.
 * Abstracts the underlying image generation implementation (Stable Diffusion API).
 */
public interface ImageAIPort {

    /**
     * Generate an image from a text prompt.
     *
     * @param prompt Description of the desired image
     * @param modelId ID of the image model to use
     * @return Path to the generated image file
     */
    Path generateImage(String prompt, String modelId);

    /**
     * Check if the image generation service is available.
     *
     * @return true if service is healthy
     */
    boolean isAvailable();
}
