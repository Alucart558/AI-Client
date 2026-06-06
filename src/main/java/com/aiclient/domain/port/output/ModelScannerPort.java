package com.aiclient.domain.port.output;

import com.aiclient.domain.model.AIModel;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Driven port for scanning and watching AI model files.
 * Abstracts file system operations for model discovery.
 */
public interface ModelScannerPort {

    /**
     * Scan a directory for AI model files (.gguf, .safetensors, etc.).
     *
     * @param directory Directory to scan
     * @return List of discovered models
     */
    List<AIModel> scanDirectory(Path directory);

    /**
     * Start watching directories for new model files.
     *
     * @param directories Directories to watch
     * @param onModelDiscovered Callback invoked when a new model is found
     */
    void startWatching(List<Path> directories, Consumer<AIModel> onModelDiscovered);

    /**
     * Stop watching directories.
     */
    void stopWatching();

    /**
     * Check if the watcher is currently active.
     *
     * @return true if watching
     */
    boolean isWatching();
}
