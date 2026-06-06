package com.aiclient.domain.port.input;

import com.aiclient.domain.model.GeneratedImage;

import java.util.List;

/**
 * Driving port for image generation functionality.
 * Defines operations for generating and managing AI-generated images.
 */
public interface ImageGenerationUseCase {

    /**
     * Generate an image from a text prompt.
     *
     * @param prompt Description of the desired image
     * @param modelId ID of the image generation model
     * @return Generated image metadata and file reference
     */
    GeneratedImage generateImage(String prompt, String modelId);

    /**
     * Retrieve all generated images.
     *
     * @return List of all generated images
     */
    List<GeneratedImage> getAllImages();

    /**
     * Permanently delete a generated image.
     *
     * @param imageId Image identifier
     */
    void deleteImage(String imageId);

    /**
     * Get a specific generated image by ID.
     *
     * @param imageId Image identifier
     * @return Generated image metadata
     */
    GeneratedImage getImage(String imageId);
}
