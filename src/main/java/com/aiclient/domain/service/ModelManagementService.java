package com.aiclient.domain.service;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.ModelType;
import com.aiclient.domain.port.input.ModelManagementUseCase;
import com.aiclient.domain.port.output.ModelScannerPort;
import com.aiclient.domain.port.output.ProcessManagementPort;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ModelManagementService implements ModelManagementUseCase {

    private final ModelScannerPort modelScannerPort;
    private final ProcessManagementPort processManagementPort;
    private final List<Path> watchedDirectories;
    private final ConcurrentHashMap<String, AIModel> discoveredModels;

    public ModelManagementService(ModelScannerPort modelScannerPort, ProcessManagementPort processManagementPort) {
        this.modelScannerPort = Objects.requireNonNull(modelScannerPort, "ModelScannerPort cannot be null");
        this.processManagementPort = Objects.requireNonNull(processManagementPort, "ProcessManagementPort cannot be null");
        this.watchedDirectories = new ArrayList<>();
        this.discoveredModels = new ConcurrentHashMap<>();
    }

    @Override
    public List<AIModel> scanAvailableModels() {
        List<AIModel> allModels = new ArrayList<>();

        for (Path directory : watchedDirectories) {
            List<AIModel> models = modelScannerPort.scanDirectory(directory);
            allModels.addAll(models);

            for (AIModel model : models) {
                discoveredModels.put(model.getId(), model);
            }
        }

        return allModels;
    }

    @Override
    public List<AIModel> getModelsByType(ModelType type) {
        Objects.requireNonNull(type, "Model type cannot be null");

        return discoveredModels.values()
                .stream()
                .filter(model -> model.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public AIModel getModel(String modelId) {
        Objects.requireNonNull(modelId, "Model ID cannot be null");

        AIModel model = discoveredModels.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }
        return model;
    }

    @Override
    public void startModelWatcher() {
        if (watchedDirectories.isEmpty()) {
            return;
        }

        modelScannerPort.startWatching(watchedDirectories, this::onModelDiscovered);
    }

    @Override
    public void stopModelWatcher() {
        modelScannerPort.stopWatching();
    }

    public void addWatchDirectory(Path directory) {
        Objects.requireNonNull(directory, "Directory cannot be null");

        if (!watchedDirectories.contains(directory)) {
            watchedDirectories.add(directory);
        }
    }

    public boolean isTextAIServiceRunning() {
        return processManagementPort.isTextAIServiceRunning();
    }

    public boolean isImageAIServiceRunning() {
        return processManagementPort.isImageAIServiceRunning();
    }

    public void startTextAIService() {
        processManagementPort.startTextAIService();
    }

    public void startImageAIService() {
        processManagementPort.startImageAIService();
    }

    public void stopAllServices() {
        processManagementPort.stopAllServices();
    }

    private void onModelDiscovered(AIModel model) {
        discoveredModels.put(model.getId(), model);
    }
}
