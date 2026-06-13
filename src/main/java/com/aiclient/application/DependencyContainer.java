package com.aiclient.application;

import com.aiclient.adapter.input.ui.*;
import com.aiclient.adapter.output.ai.*;
import com.aiclient.adapter.output.persistence.*;
import com.aiclient.adapter.output.process.*;
import com.aiclient.domain.port.input.*;
import com.aiclient.domain.port.output.*;
import com.aiclient.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dependency injection container for manual dependency wiring.
 * Manages the lifecycle of all application components following hexagonal architecture layers:
 * Configuration → Adapters → Services → Controllers
 *
 * This container uses constructor-based dependency injection to ensure all dependencies
 * are immutable and properly initialized at startup.
 */
public class DependencyContainer {

    private static final Logger logger = LoggerFactory.getLogger(DependencyContainer.class);

    // Configuration
    private final SimpleConfigurationAdapter configAdapter;

    // Persistence Layer
    private final DatabaseManager databaseManager;
    private final ChatPersistencePort chatPersistence;
    private final ImagePersistencePort imagePersistence;

    // AI Adapters
    private final TextAIPort textAI;
    private final ImageAIPort imageAI;
    private final ModelScannerPort modelScanner;

    // Process Management
    private final ProcessManagementPort processManagement;

    // Domain Services (Use Cases)
    private final ChatUseCase chatService;
    private final ImageGenerationUseCase imageService;
    private final ModelManagementUseCase modelService;

    // Controller Cache
    private final Map<Class<?>, Object> controllers = new HashMap<>();

    /**
     * Initializes all application dependencies in correct order.
     * Throws exceptions early if any critical component fails to initialize.
     */
    public DependencyContainer() {
        logger.info("Initializing application dependencies...");

        // 1. Configuration Layer
        configAdapter = new SimpleConfigurationAdapter();

        // 2. Database Layer
        String databasePath = configAdapter.get("database.path", "ai-client.db");
        logger.info("Initializing database at: {}", databasePath);
        databaseManager = new DatabaseManager(databasePath);
        databaseManager.initialize();

        // 3. Persistence Adapters
        chatPersistence = new SQLiteChatPersistenceAdapter(databaseManager);
        imagePersistence = new SQLiteImagePersistenceAdapter(databaseManager);

        // 4. AI Adapters
        String ollamaHost = configAdapter.get("ai.text.service.host", "localhost");
        String ollamaPort = configAdapter.get("ai.text.service.port", "11434");
        String ollamaUrl = String.format("http://%s:%s", ollamaHost, ollamaPort);
        logger.info("Initializing Ollama adapter at: {}", ollamaUrl);
        textAI = new OllamaTextAIAdapter(ollamaUrl, Duration.ofSeconds(60));

        String sdHost = configAdapter.get("ai.image.service.host", "localhost");
        String sdPort = configAdapter.get("ai.image.service.port", "7860");
        String sdUrl = String.format("http://%s:%s", sdHost, sdPort);
        logger.info("Initializing Stable Diffusion adapter at: {}", sdUrl);
        imageAI = new StableDiffusionImageAdapter(sdUrl, Duration.ofMinutes(5));

        modelScanner = new FileSystemModelScanner();

        // 5. Process Management
        ConfigurationService processConfig = new ConfigurationService();
        ProcessLauncher processLauncher = new DefaultProcessLauncher(processConfig);
        processManagement = new ProcessManagementAdapter(processLauncher);

        // 6. Domain Services
        logger.info("Initializing domain services...");
        chatService = new ChatService(textAI, chatPersistence);
        imageService = new ImageGenerationService(imageAI, imagePersistence);
        modelService = new ModelManagementService(modelScanner, processManagement);

        logger.info("All dependencies initialized successfully");
    }

    /**
     * Retrieves or creates a controller instance.
     * Controllers are cached as singletons within the container lifecycle.
     *
     * @param controllerClass The controller class to instantiate
     * @param <T> The controller type
     * @return The controller instance
     * @throws IllegalArgumentException if the controller class is unknown
     */
    @SuppressWarnings("unchecked")
    public <T> T getController(Class<T> controllerClass) {
        return (T) controllers.computeIfAbsent(controllerClass, clazz -> {
            logger.debug("Creating controller: {}", clazz.getSimpleName());

            if (clazz == MainController.class) {
                return new MainController(chatService, imageService, modelService);
            } else if (clazz == ChatViewController.class) {
                return new ChatViewController(chatService);
            } else if (clazz == ImageGenerationController.class) {
                return new ImageGenerationController(imageService, modelService);
            } else if (clazz == HistoryController.class) {
                return new HistoryController(chatService, imageService);
            } else if (clazz == SettingsController.class) {
                return new SettingsController(configAdapter);
            }

            throw new IllegalArgumentException("Unknown controller class: " + clazz.getName());
        });
    }

    /**
     * Gracefully shuts down all managed resources.
     * Stops model watchers, process management, and closes database connections.
     */
    public void shutdown() {
        logger.info("Shutting down application...");

        try {
            // Stop model watcher
            logger.debug("Stopping model scanner...");
            modelScanner.stopWatching();
        } catch (Exception e) {
            logger.error("Error stopping model scanner", e);
        }

        try {
            // Stop all managed processes
            logger.debug("Stopping all AI services...");
            processManagement.stopAllServices();
        } catch (Exception e) {
            logger.error("Error stopping AI services", e);
        }

        try {
            // Close database connection
            logger.debug("Closing database connection...");
            databaseManager.close();
        } catch (Exception e) {
            logger.error("Error closing database", e);
        }

        logger.info("Application shutdown complete");
    }
}
