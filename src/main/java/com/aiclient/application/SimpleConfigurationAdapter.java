package com.aiclient.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration adapter for application settings.
 * Reads configuration from application.properties file.
 *
 * This is a simplified implementation for the initial application wiring.
 * In future phases, this can be replaced with a more sophisticated ConfigurationPort implementation.
 */
public class SimpleConfigurationAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigurationAdapter.class);
    private static final String PROPERTIES_FILE = "application.properties";

    private final Properties properties;

    /**
     * Creates a new SimpleConfigurationAdapter by loading application.properties from classpath.
     */
    public SimpleConfigurationAdapter() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets a configuration value by key with a default fallback.
     *
     * @param key The configuration key
     * @param defaultValue The default value if key is not found
     * @return The configuration value or default
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets a configuration value by key.
     *
     * @param key The configuration key
     * @return The configuration value or null if not found
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Sets a configuration value (in-memory only, not persisted).
     *
     * @param key The configuration key
     * @param value The configuration value
     */
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Loads properties from the application.properties file.
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.warn("Unable to find {}, using defaults", PROPERTIES_FILE);
                return;
            }
            properties.load(input);
            logger.info("Configuration loaded from {}", PROPERTIES_FILE);
        } catch (IOException e) {
            logger.error("Error loading configuration file: {}", PROPERTIES_FILE, e);
            logger.warn("Using default configuration values");
        }
    }
}
