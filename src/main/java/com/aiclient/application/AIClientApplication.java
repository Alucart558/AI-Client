package com.aiclient.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application entry point for AI Client Desktop.
 *
 * This class manages:
 * - Application lifecycle (startup and shutdown)
 * - Dependency injection wiring via DependencyContainer
 * - Primary stage initialization
 * - Graceful resource cleanup
 *
 * Architecture: This is the composition root where all dependencies are wired together
 * following the Hexagonal Architecture pattern.
 */
public class AIClientApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(AIClientApplication.class);
    private static final String MAIN_FXML = "/fxml/main.fxml";
    private static final String APP_TITLE = "AI Client Desktop";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;

    private DependencyContainer container;

    /**
     * JavaFX application entry point.
     * Initializes the dependency container, loads the main UI, and displays the primary stage.
     *
     * @param primaryStage The primary stage provided by JavaFX
     * @throws Exception if initialization fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting AI Client Desktop application...");

        try {
            // Initialize dependency container
            logger.debug("Initializing dependency container...");
            container = new DependencyContainer();

            // Load main FXML with custom controller factory
            logger.debug("Loading main UI from: {}", MAIN_FXML);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            loader.setControllerFactory(container::getController);

            Parent root = loader.load();

            // Create and configure scene
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            primaryStage.setScene(scene);
            primaryStage.setTitle(APP_TITLE);

            // Handle window close request
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Window close requested");
                shutdown();
            });

            // Show the primary stage
            primaryStage.show();
            logger.info("Application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            // Show error dialog before exiting
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "Failed to start application: " + e.getMessage(),
                        javafx.scene.control.ButtonType.OK
                );
                alert.setTitle("Startup Error");
                alert.setHeaderText("Application Failed to Start");
                alert.showAndWait();
                Platform.exit();
            });
            throw e;
        }
    }

    /**
     * JavaFX lifecycle method called when the application is stopping.
     * Ensures graceful shutdown of all resources.
     *
     * @throws Exception if shutdown fails
     */
    @Override
    public void stop() throws Exception {
        logger.info("Application stop() called");
        shutdown();
        super.stop();
    }

    /**
     * Performs graceful shutdown of all application resources.
     * This method is safe to call multiple times.
     */
    private void shutdown() {
        if (container != null) {
            logger.debug("Initiating graceful shutdown...");
            try {
                container.shutdown();
                logger.info("Graceful shutdown completed");
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }
    }

    /**
     * Main method - standard Java application entry point.
     * Delegates to JavaFX Application.launch().
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        logger.info("Launching AI Client Desktop...");
        launch(args);
    }
}
