package com.aiclient.domain.service;

import com.aiclient.domain.model.GeneratedImage;
import com.aiclient.domain.port.output.ImageAIPort;
import com.aiclient.domain.port.output.ImagePersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageGenerationServiceTest {

    @Mock
    private ImageAIPort imageAIPort;

    @Mock
    private ImagePersistencePort imagePersistencePort;

    @InjectMocks
    private ImageGenerationService imageGenerationService;

    @Test
    void shouldRejectNullImageAIPort() {
        assertThatThrownBy(() -> new ImageGenerationService(null, imagePersistencePort))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ImageAIPort cannot be null");
    }

    @Test
    void shouldRejectNullImagePersistencePort() {
        assertThatThrownBy(() -> new ImageGenerationService(imageAIPort, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ImagePersistencePort cannot be null");
    }

    @Test
    void shouldGenerateImage() {
        String prompt = "A beautiful sunset";
        String modelId = "stable-diffusion-v1.5";
        Path generatedPath = Paths.get("generated_images/img-123.png");

        when(imageAIPort.generateImage(eq(prompt), eq(modelId))).thenReturn(generatedPath);
        when(imagePersistencePort.saveImage(any(GeneratedImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeneratedImage image = imageGenerationService.generateImage(prompt, modelId);

        assertThat(image).isNotNull();
        assertThat(image.getId()).isNotNull();
        assertThat(image.getPrompt()).isEqualTo(prompt);
        assertThat(image.getModelId()).isEqualTo(modelId);
        assertThat(image.getFilePath()).isEqualTo(generatedPath);
        assertThat(image.getGeneratedAt()).isNotNull();

        verify(imageAIPort).generateImage(prompt, modelId);
        verify(imagePersistencePort).saveImage(any(GeneratedImage.class));
    }

    @Test
    void shouldRejectNullPromptOnGenerateImage() {
        assertThatThrownBy(() -> imageGenerationService.generateImage(null, "model-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prompt cannot be null");
    }

    @Test
    void shouldRejectNullModelIdOnGenerateImage() {
        assertThatThrownBy(() -> imageGenerationService.generateImage("prompt", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldGetAllImages() {
        GeneratedImage image1 = new GeneratedImage(
                "img-1",
                "Prompt 1",
                "model-1",
                Paths.get("img1.png"),
                LocalDateTime.now()
        );
        GeneratedImage image2 = new GeneratedImage(
                "img-2",
                "Prompt 2",
                "model-2",
                Paths.get("img2.png"),
                LocalDateTime.now()
        );

        when(imagePersistencePort.findAllImages()).thenReturn(Arrays.asList(image1, image2));

        List<GeneratedImage> images = imageGenerationService.getAllImages();

        assertThat(images).hasSize(2);
        assertThat(images).containsExactly(image1, image2);

        verify(imagePersistencePort).findAllImages();
    }

    @Test
    void shouldGetImageById() {
        String imageId = "img-1";
        GeneratedImage image = new GeneratedImage(
                imageId,
                "Test prompt",
                "model-1",
                Paths.get("test.png"),
                LocalDateTime.now()
        );

        when(imagePersistencePort.findImageById(imageId)).thenReturn(Optional.of(image));

        GeneratedImage result = imageGenerationService.getImage(imageId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(imageId);

        verify(imagePersistencePort).findImageById(imageId);
    }

    @Test
    void shouldRejectNullImageIdOnGetImage() {
        assertThatThrownBy(() -> imageGenerationService.getImage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Image ID cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenImageNotFound() {
        String imageId = "nonexistent";

        when(imagePersistencePort.findImageById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageGenerationService.getImage(imageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image not found");
    }

    @Test
    void shouldDeleteImage() {
        String imageId = "img-1";

        imageGenerationService.deleteImage(imageId);

        verify(imagePersistencePort).deleteImage(imageId);
    }

    @Test
    void shouldRejectNullImageIdOnDeleteImage() {
        assertThatThrownBy(() -> imageGenerationService.deleteImage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Image ID cannot be null");
    }

    @Test
    void shouldPropagateExceptionFromImageAIPort() {
        String prompt = "Test prompt";
        String modelId = "model-1";

        when(imageAIPort.generateImage(prompt, modelId))
                .thenThrow(new RuntimeException("Image generation failed"));

        assertThatThrownBy(() -> imageGenerationService.generateImage(prompt, modelId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Image generation failed");
    }

    @Test
    void shouldPropagateExceptionFromImagePersistencePort() {
        String prompt = "Test prompt";
        String modelId = "model-1";
        Path generatedPath = Paths.get("test.png");

        when(imageAIPort.generateImage(prompt, modelId)).thenReturn(generatedPath);
        when(imagePersistencePort.saveImage(any(GeneratedImage.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> imageGenerationService.generateImage(prompt, modelId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }
}
