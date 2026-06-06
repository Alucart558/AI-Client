package com.aiclient.domain.port.output;

import com.aiclient.domain.model.GeneratedImage;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for image metadata persistence.
 * Abstracts database operations for generated images.
 */
public interface ImagePersistencePort {

    /**
     * Save generated image metadata.
     *
     * @param image Image metadata to save
     * @return Saved image with generated ID
     */
    GeneratedImage saveImage(GeneratedImage image);

    /**
     * Retrieve an image by ID.
     *
     * @param imageId Image identifier
     * @return Optional containing the image if found
     */
    Optional<GeneratedImage> findImageById(String imageId);

    /**
     * Get all generated images.
     *
     * @return List of all images
     */
    List<GeneratedImage> findAllImages();

    /**
     * Delete image metadata permanently.
     *
     * @param imageId Image identifier
     */
    void deleteImage(String imageId);
}
