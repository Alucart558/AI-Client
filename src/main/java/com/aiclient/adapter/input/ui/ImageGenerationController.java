package com.aiclient.adapter.input.ui;

import com.aiclient.domain.model.AIModel;
import com.aiclient.domain.model.GeneratedImage;
import com.aiclient.domain.model.ModelType;
import com.aiclient.domain.port.input.ImageGenerationUseCase;
import com.aiclient.domain.port.input.ModelManagementUseCase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Controller for the image generation interface.
 * Manages prompt input, model selection, and image display.
 */
public class ImageGenerationController {

    private final ImageGenerationUseCase imageService;
    private final ModelManagementUseCase modelService;

    @FXML
    private TextArea promptInput;

    @FXML
    private ComboBox<String> modelSelector;

    @FXML
    private Button generateButton;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private ImageView generatedImageView;

    @FXML
    private Label imageStatus;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor with dependency injection.
     *
     * @param imageService Image generation use case implementation
     * @param modelService Model management use case implementation
     */
    public ImageGenerationController(ImageGenerationUseCase imageService, ModelManagementUseCase modelService) {
        this.imageService = Objects.requireNonNull(imageService, "ImageService cannot be null");
        this.modelService = Objects.requireNonNull(modelService, "ModelService cannot be null");
    }

    /**
     * JavaFX lifecycle method called after FXML loading.
     * Initializes the UI state and loads available image models.
     */
    @FXML
    public void initialize() {
        // Disable generate button initially
        generateButton.setDisable(true);

        // Enable generate button only when prompt is not empty and model is selected
        promptInput.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateButtonState());
        modelSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateButtonState());

        loadImageModels();
    }

    /**
     * Handles the Generate button action.
     * Validates input and generates an image from the prompt.
     */
    @FXML
    private void handleGenerate() {
        String prompt = promptInput.getText().trim();
        String modelId = modelSelector.getValue();

        if (prompt.isEmpty()) {
            showError("Please enter a prompt");
            return;
        }

        if (modelId == null || modelId.isEmpty()) {
            showError("Please select a model");
            return;
        }

        // Show progress indicator and disable button
        progressIndicator.setVisible(true);
        generateButton.setDisable(true);
        imageStatus.setText("Generating image...");

        // Generate image in background thread
        new Thread(() -> {
            try {
                GeneratedImage image = imageService.generateImage(prompt, modelId);
                Platform.runLater(() -> {
                    displayImage(image);
                    progressIndicator.setVisible(false);
                    updateGenerateButtonState();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to generate image: " + e.getMessage());
                    progressIndicator.setVisible(false);
                    updateGenerateButtonState();
                });
            }
        }).start();
    }

    /**
     * Displays a generated image in the ImageView.
     *
     * @param image Generated image metadata
     */
    private void displayImage(GeneratedImage image) {
        try {
            String imagePath = image.getFilePath().toAbsolutePath().toString();
            // Use file:/// protocol for local files
            Image fxImage = new Image("file:///" + imagePath);
            generatedImageView.setImage(fxImage);

            String timestamp = image.getGeneratedAt().format(DATE_FORMATTER);
            imageStatus.setText("Generated: " + timestamp);
        } catch (Exception e) {
            showError("Failed to display image: " + e.getMessage());
        }
    }

    /**
     * Loads available image generation models into the ComboBox.
     */
    private void loadImageModels() {
        new Thread(() -> {
            try {
                List<AIModel> models = modelService.getModelsByType(ModelType.IMAGE);
                Platform.runLater(() -> {
                    modelSelector.getItems().clear();
                    for (AIModel model : models) {
                        modelSelector.getItems().add(model.getId());
                    }
                    if (!models.isEmpty()) {
                        modelSelector.getSelectionModel().selectFirst();
                    }
                    updateGenerateButtonState();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load models: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Updates the generate button state based on input validation.
     */
    private void updateGenerateButtonState() {
        boolean hasPrompt = !promptInput.getText().trim().isEmpty();
        boolean hasModel = modelSelector.getValue() != null && !modelSelector.getValue().isEmpty();
        boolean isNotGenerating = !progressIndicator.isVisible();
        generateButton.setDisable(!hasPrompt || !hasModel || !isNotGenerating);
    }

    /**
     * Displays an error alert dialog.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
        imageStatus.setText("Error: " + message);
    }
}
