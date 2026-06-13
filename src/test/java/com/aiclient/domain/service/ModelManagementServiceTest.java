package com.aiclient.domain.service;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.ModelType;
import com.aiclient.domain.port.output.ModelScannerPort;
import com.aiclient.domain.port.output.ProcessManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ModelManagementServiceTest {

    @Mock
    private ModelScannerPort modelScannerPort;

    @Mock
    private ProcessManagementPort processManagementPort;

    private ModelManagementService modelManagementService;

    @BeforeEach
    void setUp() {
        modelManagementService = new ModelManagementService(modelScannerPort, processManagementPort);
    }

    @Test
    void shouldRejectNullModelScannerPort() {
        assertThatThrownBy(() -> new ModelManagementService(null, processManagementPort))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ModelScannerPort cannot be null");
    }

    @Test
    void shouldRejectNullProcessManagementPort() {
        assertThatThrownBy(() -> new ModelManagementService(modelScannerPort, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ProcessManagementPort cannot be null");
    }

    @Test
    void shouldScanAvailableModels() {
        Path directory = Paths.get("/models");
        AIModel model1 = new AIModel("model-1", "Llama 2", ModelType.TEXT, Paths.get("/models/llama2.gguf"), 1000L);
        AIModel model2 = new AIModel("model-2", "SD 1.5", ModelType.IMAGE, Paths.get("/models/sd.safetensors"), 2000L);

        modelManagementService.addWatchDirectory(directory);
        when(modelScannerPort.scanDirectory(directory)).thenReturn(Arrays.asList(model1, model2));

        List<AIModel> models = modelManagementService.scanAvailableModels();

        assertThat(models).hasSize(2);
        assertThat(models).containsExactly(model1, model2);

        verify(modelScannerPort).scanDirectory(directory);
    }

    @Test
    void shouldReturnEmptyListWhenNoDirectoriesAdded() {
        List<AIModel> models = modelManagementService.scanAvailableModels();

        assertThat(models).isEmpty();
    }

    @Test
    void shouldScanMultipleDirectories() {
        Path dir1 = Paths.get("/models/text");
        Path dir2 = Paths.get("/models/image");

        AIModel textModel = new AIModel("model-1", "Llama", ModelType.TEXT, Paths.get("/models/text/llama.gguf"), 1000L);
        AIModel imageModel = new AIModel("model-2", "SD", ModelType.IMAGE, Paths.get("/models/image/sd.safetensors"), 2000L);

        modelManagementService.addWatchDirectory(dir1);
        modelManagementService.addWatchDirectory(dir2);

        when(modelScannerPort.scanDirectory(dir1)).thenReturn(List.of(textModel));
        when(modelScannerPort.scanDirectory(dir2)).thenReturn(List.of(imageModel));

        List<AIModel> models = modelManagementService.scanAvailableModels();

        assertThat(models).hasSize(2);
        verify(modelScannerPort).scanDirectory(dir1);
        verify(modelScannerPort).scanDirectory(dir2);
    }

    @Test
    void shouldGetModelsByType() {
        Path directory = Paths.get("/models");
        AIModel textModel = new AIModel("model-1", "Llama", ModelType.TEXT, Paths.get("/models/llama.gguf"), 1000L);
        AIModel imageModel = new AIModel("model-2", "SD", ModelType.IMAGE, Paths.get("/models/sd.safetensors"), 2000L);

        modelManagementService.addWatchDirectory(directory);
        when(modelScannerPort.scanDirectory(directory)).thenReturn(Arrays.asList(textModel, imageModel));

        modelManagementService.scanAvailableModels();

        List<AIModel> textModels = modelManagementService.getModelsByType(ModelType.TEXT);
        List<AIModel> imageModels = modelManagementService.getModelsByType(ModelType.IMAGE);

        assertThat(textModels).hasSize(1);
        assertThat(textModels.get(0).getType()).isEqualTo(ModelType.TEXT);

        assertThat(imageModels).hasSize(1);
        assertThat(imageModels.get(0).getType()).isEqualTo(ModelType.IMAGE);
    }

    @Test
    void shouldRejectNullModelTypeOnGetModelsByType() {
        assertThatThrownBy(() -> modelManagementService.getModelsByType(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model type cannot be null");
    }

    @Test
    void shouldGetModelById() {
        Path directory = Paths.get("/models");
        AIModel model = new AIModel("model-1", "Llama", ModelType.TEXT, Paths.get("/models/llama.gguf"), 1000L);

        modelManagementService.addWatchDirectory(directory);
        when(modelScannerPort.scanDirectory(directory)).thenReturn(List.of(model));

        modelManagementService.scanAvailableModels();

        AIModel result = modelManagementService.getModel("model-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("model-1");
    }

    @Test
    void shouldRejectNullModelIdOnGetModel() {
        assertThatThrownBy(() -> modelManagementService.getModel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Model ID cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenModelNotFound() {
        assertThatThrownBy(() -> modelManagementService.getModel("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not found");
    }

    @Test
    void shouldStartModelWatcher() {
        Path directory = Paths.get("/models");
        modelManagementService.addWatchDirectory(directory);

        modelManagementService.startModelWatcher();

        verify(modelScannerPort).startWatching(anyList(), any(Consumer.class));
    }

    @Test
    void shouldNotStartWatcherWhenNoDirectories() {
        modelManagementService.startModelWatcher();

        verify(modelScannerPort, never()).startWatching(anyList(), any(Consumer.class));
    }

    @Test
    void shouldStopModelWatcher() {
        modelManagementService.stopModelWatcher();

        verify(modelScannerPort).stopWatching();
    }

    @Test
    void shouldAddWatchDirectory() {
        Path directory = Paths.get("/models");

        modelManagementService.addWatchDirectory(directory);
        modelManagementService.addWatchDirectory(directory);

        when(modelScannerPort.scanDirectory(directory)).thenReturn(List.of());

        modelManagementService.scanAvailableModels();

        verify(modelScannerPort).scanDirectory(directory);
    }

    @Test
    void shouldRejectNullDirectoryOnAddWatchDirectory() {
        assertThatThrownBy(() -> modelManagementService.addWatchDirectory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Directory cannot be null");
    }

    @Test
    void shouldCheckIfTextAIServiceIsRunning() {
        when(processManagementPort.isTextAIServiceRunning()).thenReturn(true);

        boolean running = modelManagementService.isTextAIServiceRunning();

        assertThat(running).isTrue();
        verify(processManagementPort).isTextAIServiceRunning();
    }

    @Test
    void shouldCheckIfImageAIServiceIsRunning() {
        when(processManagementPort.isImageAIServiceRunning()).thenReturn(false);

        boolean running = modelManagementService.isImageAIServiceRunning();

        assertThat(running).isFalse();
        verify(processManagementPort).isImageAIServiceRunning();
    }

    @Test
    void shouldStartTextAIService() {
        modelManagementService.startTextAIService();

        verify(processManagementPort).startTextAIService();
    }

    @Test
    void shouldStartImageAIService() {
        modelManagementService.startImageAIService();

        verify(processManagementPort).startImageAIService();
    }

    @Test
    void shouldStopAllServices() {
        modelManagementService.stopAllServices();

        verify(processManagementPort).stopAllServices();
    }

    @Test
    void shouldPropagateExceptionFromProcessManagementPort() {
        when(processManagementPort.isTextAIServiceRunning())
                .thenThrow(new RuntimeException("Process check failed"));

        assertThatThrownBy(() -> modelManagementService.isTextAIServiceRunning())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Process check failed");
    }

    @Test
    void shouldPropagateExceptionFromModelScannerPort() {
        Path directory = Paths.get("/models");
        modelManagementService.addWatchDirectory(directory);

        when(modelScannerPort.scanDirectory(directory))
                .thenThrow(new RuntimeException("Scan failed"));

        assertThatThrownBy(() -> modelManagementService.scanAvailableModels())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Scan failed");
    }
}
