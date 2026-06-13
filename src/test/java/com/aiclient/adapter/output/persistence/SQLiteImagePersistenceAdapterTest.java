package com.aiclient.adapter.output.persistence;

import com.aiclient.domain.model.GeneratedImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SQLiteImagePersistenceAdapterTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private SQLiteImagePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        String dbPath = tempDir.resolve("test.db").toString();
        databaseManager = new DatabaseManager(dbPath);
        adapter = new SQLiteImagePersistenceAdapter(databaseManager);
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
        assertThatThrownBy(() -> new SQLiteImagePersistenceAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DatabaseManager cannot be null");
    }

    @Test
    void shouldSaveImage() {
        GeneratedImage image = new GeneratedImage(
                "img-1",
                "A beautiful landscape",
                "stable-diffusion",
                Paths.get("/path/to/image.png"),
                LocalDateTime.now()
        );

        GeneratedImage saved = adapter.saveImage(image);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("img-1");
        assertThat(saved.getPrompt()).isEqualTo("A beautiful landscape");
    }

    @Test
    void shouldRejectNullImageOnSave() {
        assertThatThrownBy(() -> adapter.saveImage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Image cannot be null");
    }

    @Test
    void shouldFindImageById() {
        GeneratedImage image = new GeneratedImage(
                "img-1",
                "Test prompt",
                "sd-model",
                Paths.get("/test.png"),
                LocalDateTime.now()
        );

        adapter.saveImage(image);

        Optional<GeneratedImage> found = adapter.findImageById("img-1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("img-1");
        assertThat(found.get().getPrompt()).isEqualTo("Test prompt");
    }

    @Test
    void shouldReturnEmptyWhenImageNotFound() {
        Optional<GeneratedImage> found = adapter.findImageById("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldRejectNullImageIdOnFind() {
        assertThatThrownBy(() -> adapter.findImageById(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Image ID cannot be null");
    }

    @Test
    void shouldFindAllImages() {
        GeneratedImage img1 = new GeneratedImage(
                "img-1",
                "Prompt 1",
                "model-1",
                Paths.get("/img1.png"),
                LocalDateTime.now()
        );

        GeneratedImage img2 = new GeneratedImage(
                "img-2",
                "Prompt 2",
                "model-2",
                Paths.get("/img2.png"),
                LocalDateTime.now().plusMinutes(1)
        );

        adapter.saveImage(img1);
        adapter.saveImage(img2);

        List<GeneratedImage> images = adapter.findAllImages();

        assertThat(images).hasSize(2);
        assertThat(images).extracting(GeneratedImage::getId)
                .containsExactlyInAnyOrder("img-1", "img-2");
    }

    @Test
    void shouldReturnEmptyListWhenNoImages() {
        List<GeneratedImage> images = adapter.findAllImages();
        assertThat(images).isEmpty();
    }

    @Test
    void shouldDeleteImage() {
        GeneratedImage image = new GeneratedImage(
                "img-1",
                "Test",
                "model",
                Paths.get("/test.png"),
                LocalDateTime.now()
        );

        adapter.saveImage(image);

        adapter.deleteImage("img-1");

        Optional<GeneratedImage> found = adapter.findImageById("img-1");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldHandleDeleteNonexistentImage() {
        adapter.deleteImage("nonexistent");
    }

    @Test
    void shouldRejectNullImageIdOnDelete() {
        assertThatThrownBy(() -> adapter.deleteImage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Image ID cannot be null");
    }

    @Test
    void shouldPreserveFilePath() {
        Path originalPath = Paths.get("/path/to/my/image.png");
        GeneratedImage image = new GeneratedImage(
                "img-1",
                "Test",
                "model",
                originalPath,
                LocalDateTime.now()
        );

        adapter.saveImage(image);

        Optional<GeneratedImage> found = adapter.findImageById("img-1");
        assertThat(found).isPresent();
        assertThat(found.get().getFilePath().toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void shouldUpdateImageOnReplaceInsert() {
        GeneratedImage original = new GeneratedImage(
                "img-1",
                "Original prompt",
                "model-1",
                Paths.get("/original.png"),
                LocalDateTime.now()
        );

        adapter.saveImage(original);

        GeneratedImage updated = new GeneratedImage(
                "img-1",
                "Updated prompt",
                "model-2",
                Paths.get("/updated.png"),
                LocalDateTime.now()
        );

        adapter.saveImage(updated);

        Optional<GeneratedImage> found = adapter.findImageById("img-1");
        assertThat(found).isPresent();
        assertThat(found.get().getPrompt()).isEqualTo("Updated prompt");
        assertThat(found.get().getModelId()).isEqualTo("model-2");
    }

    @Test
    void shouldOrderImagesByGeneratedAtDesc() {
        LocalDateTime now = LocalDateTime.now();

        GeneratedImage img1 = new GeneratedImage("img-1", "First", "model", Paths.get("/1.png"), now);
        GeneratedImage img2 = new GeneratedImage("img-2", "Second", "model", Paths.get("/2.png"), now.plusMinutes(1));
        GeneratedImage img3 = new GeneratedImage("img-3", "Third", "model", Paths.get("/3.png"), now.plusMinutes(2));

        adapter.saveImage(img1);
        adapter.saveImage(img3);
        adapter.saveImage(img2);

        List<GeneratedImage> images = adapter.findAllImages();

        assertThat(images).hasSize(3);
        assertThat(images.get(0).getId()).isEqualTo("img-3");
        assertThat(images.get(1).getId()).isEqualTo("img-2");
        assertThat(images.get(2).getId()).isEqualTo("img-1");
    }
}
