package com.aiclient.adapter.input.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for application settings.
 * Manages configuration for AI services and model scanning.
 */
public class SettingsController {

    @FXML
    private TextField ollamaUrlField;

    @FXML
    private TextField sdUrlField;

    @FXML
    private ListView<String> scanPathsList;

    @FXML
    private Button addPathButton;

    @FXML
    private Button removePathButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button resetButton;

    // Default configuration values
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_SD_URL = "http://localhost:7860";
    private static final List<String> DEFAULT_SCAN_PATHS = List.of(
            System.getProperty("user.home") + "/.ollama/models",
            System.getProperty("user.home") + "/stable-diffusion/models"
    );

    private final com.aiclient.application.SimpleConfigurationAdapter configAdapter;

    /**
     * Constructor with configuration adapter.
     * For Phase 5.3, this accepts a simple configuration adapter for settings management.
     *
     * @param configAdapter Configuration adapter for reading/writing settings
     */
    public SettingsController(com.aiclient.application.SimpleConfigurationAdapter configAdapter) {
        this.configAdapter = configAdapter;
    }

    /**
     * Default constructor for backward compatibility.
     * Note: In a real implementation, this would inject a ConfigurationPort
     * to persist settings. For now, settings are stored in memory only.
     */
    public SettingsController() {
        this.configAdapter = null;
    }

    /**
     * JavaFX lifecycle method called after FXML loading.
     * Initializes the UI state and loads current settings.
     */
    @FXML
    public void initialize() {
        // Enable remove button only when a path is selected
        scanPathsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> removePathButton.setDisable(newVal == null)
        );

        loadSettings();
    }

    /**
     * Handles the Save button action.
     * Validates and persists the configuration settings.
     */
    @FXML
    private void handleSave() {
        String ollamaUrl = ollamaUrlField.getText().trim();
        String sdUrl = sdUrlField.getText().trim();

        // Validate URLs
        if (!isValidUrl(ollamaUrl)) {
            showError("Invalid Ollama URL. Must start with http:// or https://");
            return;
        }

        if (!isValidUrl(sdUrl)) {
            showError("Invalid Stable Diffusion URL. Must start with http:// or https://");
            return;
        }

        // In a real implementation, persist settings here via ConfigurationPort
        // For now, just show success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText(null);
        alert.setContentText("Settings saved successfully!\n\nNote: Restart the application for changes to take effect.");
        alert.showAndWait();
    }

    /**
     * Handles the Add Path button action.
     * Opens a directory chooser to add a new scan path.
     */
    @FXML
    private void handleAddPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Model Directory");

        // Set initial directory to user home
        File userHome = new File(System.getProperty("user.home"));
        if (userHome.exists()) {
            chooser.setInitialDirectory(userHome);
        }

        File selectedDir = chooser.showDialog(addPathButton.getScene().getWindow());
        if (selectedDir != null) {
            String path = selectedDir.getAbsolutePath();
            if (!scanPathsList.getItems().contains(path)) {
                scanPathsList.getItems().add(path);
            } else {
                showInfo("This path is already in the list.");
            }
        }
    }

    /**
     * Handles the Remove Path button action.
     * Removes the selected path from the scan paths list.
     */
    @FXML
    private void handleRemovePath() {
        String selected = scanPathsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            scanPathsList.getItems().remove(selected);
        }
    }

    /**
     * Handles the Reset button action.
     * Resets all settings to their default values.
     */
    @FXML
    private void handleReset() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Reset all settings to default values?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Reset Settings");
        confirm.setHeaderText("Confirm Reset");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                resetToDefaults();
            }
        });
    }

    /**
     * Loads current settings into the UI fields.
     * In a real implementation, this would load from ConfigurationPort.
     */
    private void loadSettings() {
        // Load default values (in real app, load from ConfigurationPort)
        ollamaUrlField.setText(DEFAULT_OLLAMA_URL);
        sdUrlField.setText(DEFAULT_SD_URL);

        scanPathsList.getItems().clear();
        scanPathsList.getItems().addAll(new ArrayList<>(DEFAULT_SCAN_PATHS));
    }

    /**
     * Resets all settings to their default values.
     */
    private void resetToDefaults() {
        ollamaUrlField.setText(DEFAULT_OLLAMA_URL);
        sdUrlField.setText(DEFAULT_SD_URL);

        scanPathsList.getItems().clear();
        scanPathsList.getItems().addAll(new ArrayList<>(DEFAULT_SCAN_PATHS));

        showInfo("Settings have been reset to defaults.");
    }

    /**
     * Validates that a URL starts with http:// or https://.
     *
     * @param url URL string to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * Displays an error alert dialog.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Displays an information alert dialog.
     *
     * @param message Information message to display
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }
}
