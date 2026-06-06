package com.aiclient.domain.port.input;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.ModelType;

import java.util.List;

/**
 * Driving port for AI model management.
 * Defines operations for discovering and managing local AI models.
 */
public interface ModelManagementUseCase {

    /**
     * Scan local directories for available AI models.
     *
     * @return List of discovered models
     */
    List<AIModel> scanAvailableModels();

    /**
     * Get models filtered by type (TEXT, IMAGE).
     *
     * @param type Model type filter
     * @return List of models matching the type
     */
    List<AIModel> getModelsByType(ModelType type);

    /**
     * Get a specific model by its ID.
     *
     * @param modelId Model identifier
     * @return Model metadata
     */
    AIModel getModel(String modelId);

    /**
     * Start watching model directories for changes.
     * New models added to watched folders will be automatically detected.
     */
    void startModelWatcher();

    /**
     * Stop watching model directories.
     */
    void stopModelWatcher();
}
