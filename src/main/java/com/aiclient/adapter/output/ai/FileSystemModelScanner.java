package com.aiclient.adapter.output.ai;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.ModelType;
import com.aiclient.domain.port.output.ModelScannerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Adapter implementing ModelScannerPort for local file system.
 * Scans directories for AI model files (.gguf, .safetensors) and watches for changes.
 */
public class FileSystemModelScanner implements ModelScannerPort {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemModelScanner.class);
    private static final List<String> MODEL_EXTENSIONS = List.of(".gguf", ".safetensors", ".bin", ".pth");

    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    private ExecutorService watcherExecutor;
    private WatchService watchService;

    @Override
    public List<AIModel> scanDirectory(Path directory) {
        Objects.requireNonNull(directory, "Directory cannot be null");

        if (!Files.exists(directory)) {
            logger.warn("Directory does not exist: {}", directory);
            return List.of();
        }

        if (!Files.isDirectory(directory)) {
            logger.warn("Path is not a directory: {}", directory);
            return List.of();
        }

        logger.debug("Scanning directory for models: {}", directory);

        List<AIModel> models = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory, 3)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isModelFile)
                    .forEach(path -> {
                        try {
                            AIModel model = createModelFromFile(path);
                            models.add(model);
                            logger.debug("Found model: {}", model.getName());
                        } catch (IOException e) {
                            logger.warn("Failed to read model file {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.error("Error scanning directory {}: {}", directory, e.getMessage());
        }

        logger.info("Found {} models in directory: {}", models.size(), directory);
        return models;
    }

    @Override
    public void startWatching(List<Path> directories, Consumer<AIModel> onModelDiscovered) {
        Objects.requireNonNull(directories, "Directories cannot be null");
        Objects.requireNonNull(onModelDiscovered, "Callback cannot be null");

        if (isWatching.get()) {
            logger.warn("Already watching directories");
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();

            for (Path directory : directories) {
                if (Files.exists(directory) && Files.isDirectory(directory)) {
                    directory.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    logger.debug("Watching directory: {}", directory);
                }
            }

            isWatching.set(true);
            watcherExecutor = Executors.newSingleThreadExecutor();
            watcherExecutor.submit(() -> watchLoop(onModelDiscovered));

            logger.info("Started watching {} directories for model changes", directories.size());
        } catch (IOException e) {
            logger.error("Failed to start watching directories: {}", e.getMessage());
            throw new RuntimeException("Failed to start watching: " + e.getMessage(), e);
        }
    }

    @Override
    public void stopWatching() {
        if (!isWatching.get()) {
            logger.debug("Not currently watching");
            return;
        }

        isWatching.set(false);

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Error closing watch service: {}", e.getMessage());
            }
        }

        if (watcherExecutor != null) {
            watcherExecutor.shutdown();
        }

        logger.info("Stopped watching directories");
    }

    @Override
    public boolean isWatching() {
        return isWatching.get();
    }

    private void watchLoop(Consumer<AIModel> onModelDiscovered) {
        while (isWatching.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    Path directory = (Path) key.watchable();
                    Path fullPath = directory.resolve(filename);

                    if (isModelFile(fullPath)) {
                        logger.debug("Detected model file: {}", fullPath);
                        try {
                            AIModel model = createModelFromFile(fullPath);
                            onModelDiscovered.accept(model);
                        } catch (IOException e) {
                            logger.warn("Failed to process detected model {}: {}", fullPath, e.getMessage());
                        }
                    }
                }

                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Watch loop interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error in watch loop: {}", e.getMessage());
            }
        }
    }

    private boolean isModelFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return MODEL_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private AIModel createModelFromFile(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        String modelId = fileName.substring(0, fileName.lastIndexOf('.'));
        ModelType type = determineModelType(fileName);
        long fileSize = Files.size(path);

        return new AIModel(modelId, fileName, type, path, fileSize);
    }

    private ModelType determineModelType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.contains("llama") || lowerFileName.contains("mistral") ||
                lowerFileName.contains("gpt") || lowerFileName.endsWith(".gguf")) {
            return ModelType.TEXT;
        } else if (lowerFileName.contains("stable-diffusion") || lowerFileName.contains("sd") ||
                lowerFileName.endsWith(".safetensors")) {
            return ModelType.IMAGE;
        }
        return ModelType.TEXT;
    }
}
