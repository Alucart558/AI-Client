package com.aiclient.adapter.input.ui;

import com.aiclient.domain.port.input.ChatUseCase;
import com.aiclient.domain.port.input.ImageGenerationUseCase;
import com.aiclient.domain.port.input.ModelManagementUseCase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

import java.util.Objects;

/**
 * Main application controller managing the shell and navigation.
 * Provides tab-based navigation between Chat, Image Generation, History, and Settings.
 */
public class MainController {

    private final ChatUseCase chatService;
    private final ImageGenerationUseCase imageService;
    private final ModelManagementUseCase modelService;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label statusLabel;

    /**
     * Constructor with dependency injection.
     *
     * @param chatService Chat use case implementation
     * @param imageService Image generation use case implementation
     * @param modelService Model management use case implementation
     */
    public MainController(ChatUseCase chatService, ImageGenerationUseCase imageService, ModelManagementUseCase modelService) {
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.imageService = Objects.requireNonNull(imageService, "ImageService cannot be null");
        this.modelService = Objects.requireNonNull(modelService, "ModelService cannot be null");
    }

    /**
     * JavaFX lifecycle method called after FXML loading.
     * Initializes the UI state.
     */
    @FXML
    public void initialize() {
        updateStatus("Ready");

        // Start model watcher in background
        new Thread(() -> {
            try {
                modelService.startModelWatcher();
                Platform.runLater(() -> updateStatus("Model watcher started"));
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("Warning: Model watcher failed to start"));
            }
        }).start();
    }

    /**
     * Handles the Exit menu action.
     * Closes the application gracefully.
     */
    @FXML
    private void handleExit() {
        try {
            modelService.stopModelWatcher();
        } catch (Exception e) {
            // Log but don't prevent exit
        }
        Platform.exit();
    }

    /**
     * Handles the About menu action.
     * Displays information about the application.
     */
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About AI Client");
        alert.setHeaderText("AI Client Desktop");
        alert.setContentText(
                "Version: 1.0.0\n" +
                "A 100% local AI application supporting:\n" +
                "- Text chat via Ollama\n" +
                "- Image generation via Stable Diffusion\n\n" +
                "Built with JavaFX and hexagonal architecture."
        );
        alert.showAndWait();
    }

    /**
     * Updates the status bar text.
     *
     * @param message Status message to display
     */
    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
