package com.aiclient.adapter.output.ai;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.ModelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemModelScannerTest {

    private FileSystemModelScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new FileSystemModelScanner();
    }

    @AfterEach
    void tearDown() {
        if (scanner.isWatching()) {
            scanner.stopWatching();
        }
    }

    @Test
    void shouldCreateScanner() {
        assertThat(scanner).isNotNull();
        assertThat(scanner.isWatching()).isFalse();
    }

    @Test
    void shouldRejectNullDirectory() {
        assertThatThrownBy(() -> scanner.scanDirectory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Directory cannot be null");
    }

    @Test
    void shouldReturnEmptyListForNonexistentDirectory() {
        Path nonexistent = tempDir.resolve("nonexistent");
        List<AIModel> models = scanner.scanDirectory(nonexistent);
        assertThat(models).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForFileInsteadOfDirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        List<AIModel> models = scanner.scanDirectory(file);
        assertThat(models).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmptyDirectory() {
        List<AIModel> models = scanner.scanDirectory(tempDir);
        assertThat(models).isEmpty();
    }

    @Test
    void shouldFindGgufModel() throws IOException {
        Path modelFile = tempDir.resolve("llama2.gguf");
        Files.writeString(modelFile, "fake model data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).hasSize(1);
        AIModel model = models.get(0);
        assertThat(model.getId()).isEqualTo("llama2");
        assertThat(model.getName()).isEqualTo("llama2.gguf");
        assertThat(model.getType()).isEqualTo(ModelType.TEXT);
        assertThat(model.getFilePath()).isEqualTo(modelFile);
    }

    @Test
    void shouldFindSafetensorsModel() throws IOException {
        Path modelFile = tempDir.resolve("stable-diffusion.safetensors");
        Files.writeString(modelFile, "fake model data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).hasSize(1);
        AIModel model = models.get(0);
        assertThat(model.getId()).isEqualTo("stable-diffusion");
        assertThat(model.getName()).isEqualTo("stable-diffusion.safetensors");
        assertThat(model.getType()).isEqualTo(ModelType.IMAGE);
    }

    @Test
    void shouldFindMultipleModels() throws IOException {
        Files.writeString(tempDir.resolve("model1.gguf"), "data");
        Files.writeString(tempDir.resolve("model2.safetensors"), "data");
        Files.writeString(tempDir.resolve("model3.bin"), "data");
        Files.writeString(tempDir.resolve("not-a-model.txt"), "data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).hasSize(3);
        assertThat(models).extracting(AIModel::getName)
                .containsExactlyInAnyOrder("model1.gguf", "model2.safetensors", "model3.bin");
    }

    @Test
    void shouldFindModelsInSubdirectories() throws IOException {
        Path subdir = tempDir.resolve("models");
        Files.createDirectories(subdir);
        Files.writeString(subdir.resolve("llama.gguf"), "data");
        Files.writeString(tempDir.resolve("gpt.gguf"), "data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).hasSize(2);
    }

    @Test
    void shouldRejectNullDirectoriesForWatching() {
        assertThatThrownBy(() -> scanner.startWatching(null, model -> {}))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Directories cannot be null");
    }

    @Test
    void shouldRejectNullCallbackForWatching() {
        assertThatThrownBy(() -> scanner.startWatching(List.of(tempDir), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Callback cannot be null");
    }

    @Test
    void shouldStartWatching() {
        scanner.startWatching(List.of(tempDir), model -> {});
        assertThat(scanner.isWatching()).isTrue();
    }

    @Test
    void shouldStopWatching() {
        scanner.startWatching(List.of(tempDir), model -> {});
        assertThat(scanner.isWatching()).isTrue();

        scanner.stopWatching();
        assertThat(scanner.isWatching()).isFalse();
    }

    @Test
    void shouldNotStartWatchingTwice() {
        scanner.startWatching(List.of(tempDir), model -> {});
        scanner.startWatching(List.of(tempDir), model -> {});
        assertThat(scanner.isWatching()).isTrue();
    }

    @Test
    void shouldHandleStopWatchingWhenNotWatching() {
        assertThat(scanner.isWatching()).isFalse();
        scanner.stopWatching();
        assertThat(scanner.isWatching()).isFalse();
    }

    @Test
    void shouldDetectNewModelFile() throws IOException, InterruptedException {
        List<AIModel> discoveredModels = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        scanner.startWatching(List.of(tempDir), model -> {
            discoveredModels.add(model);
            latch.countDown();
        });

        Thread.sleep(100);

        Path newModel = tempDir.resolve("new-model.gguf");
        Files.writeString(newModel, "model data");

        boolean detected = latch.await(2, TimeUnit.SECONDS);
        scanner.stopWatching();

        assertThat(detected).isTrue();
        assertThat(discoveredModels).isNotEmpty();
        assertThat(discoveredModels.get(0).getName()).isEqualTo("new-model.gguf");
    }

    @Test
    void shouldIgnoreNonModelFiles() throws IOException, InterruptedException {
        List<AIModel> discoveredModels = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        scanner.startWatching(List.of(tempDir), model -> {
            discoveredModels.add(model);
            latch.countDown();
        });

        Thread.sleep(100);

        Files.writeString(tempDir.resolve("readme.txt"), "not a model");

        boolean detected = latch.await(1, TimeUnit.SECONDS);
        scanner.stopWatching();

        assertThat(detected).isFalse();
        assertThat(discoveredModels).isEmpty();
    }

    @Test
    void shouldDetermineTextModelType() throws IOException {
        Files.writeString(tempDir.resolve("llama-model.gguf"), "data");
        Files.writeString(tempDir.resolve("mistral-7b.gguf"), "data");
        Files.writeString(tempDir.resolve("gpt-3.5.bin"), "data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).allMatch(model -> model.getType() == ModelType.TEXT);
    }

    @Test
    void shouldDetermineImageModelType() throws IOException {
        Files.writeString(tempDir.resolve("stable-diffusion-xl.safetensors"), "data");
        Files.writeString(tempDir.resolve("sd-v1.5.safetensors"), "data");

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).allMatch(model -> model.getType() == ModelType.IMAGE);
    }

    @Test
    void shouldRecordFileSize() throws IOException {
        String content = "model data with some length";
        Path modelFile = tempDir.resolve("model.gguf");
        Files.writeString(modelFile, content);

        List<AIModel> models = scanner.scanDirectory(tempDir);

        assertThat(models).hasSize(1);
        assertThat(models.get(0).getFileSizeBytes()).isGreaterThan(0);
    }
}
