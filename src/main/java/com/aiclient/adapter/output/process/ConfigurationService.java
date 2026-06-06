package com.aiclient.adapter.output.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Service for loading and accessing application configuration properties.
 * Reads configuration from application.properties file in classpath.
 *
 * <p>This service provides type-safe access to process management configuration
 * including AI service commands and timeout settings.</p>
 */
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String PROPERTY_OLLAMA_COMMAND = "process.ollama.command";
    private static final String PROPERTY_SD_COMMAND = "process.sd.command";
    private static final String PROPERTY_STARTUP_TIMEOUT = "process.startup.timeout.seconds";

    private final Properties properties;

    /**
     * Creates a new ConfigurationService by loading application.properties from the classpath.
     *
     * @throws IllegalStateException if the properties file cannot be loaded
     */
    public ConfigurationService() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Loads properties from the application.properties file.
     *
     * @throws IllegalStateException if properties file cannot be loaded
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.error("Unable to find {}", PROPERTIES_FILE);
                throw new IllegalStateException("Configuration file not found: " + PROPERTIES_FILE);
            }
            properties.load(input);
            logger.debug("Configuration loaded from {}", PROPERTIES_FILE);
        } catch (IOException e) {
            logger.error("Error loading configuration file: {}", PROPERTIES_FILE, e);
            throw new IllegalStateException("Failed to load configuration file", e);
        }
    }

    /**
     * Gets the command for starting the Ollama text AI service.
     *
     * @return the Ollama command string
     * @throws IllegalStateException if property is missing
     */
    public String getOllamaCommand() {
        return getRequiredProperty(PROPERTY_OLLAMA_COMMAND);
    }

    /**
     * Gets the command for starting the Stable Diffusion image AI service.
     *
     * @return the Stable Diffusion command string
     * @throws IllegalStateException if property is missing
     */
    public String getSdCommand() {
        return getRequiredProperty(PROPERTY_SD_COMMAND);
    }

    /**
     * Gets the startup timeout in seconds for AI services.
     *
     * @return timeout in seconds (defaults to 30 if not configured)
     */
    public long getStartupTimeoutSeconds() {
        String value = properties.getProperty(PROPERTY_STARTUP_TIMEOUT, "30");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid startup timeout value '{}', using default 30 seconds", value);
            return 30L;
        }
    }

    /**
     * Gets a required property value.
     *
     * @param key the property key
     * @return the property value
     * @throws IllegalStateException if property is missing
     */
    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            logger.error("Required property '{}' is missing or empty", key);
            throw new IllegalStateException("Required configuration property missing: " + key);
        }
        return value.trim();
    }
}
