package com.aiclient.adapter.output.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation of ProcessLauncher that creates real system processes.
 * Reads commands from ConfigurationService and launches processes using ProcessBuilder.
 *
 * <p>This implementation provides the production behavior for launching AI service processes.
 * Tests can inject mock ProcessLauncher implementations to avoid starting real processes.</p>
 */
public class DefaultProcessLauncher implements ProcessLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProcessLauncher.class);

    private final ConfigurationService configurationService;

    /**
     * Creates a new DefaultProcessLauncher with the given configuration service.
     *
     * @param configurationService the configuration service for reading commands
     * @throws NullPointerException if configurationService is null
     */
    public DefaultProcessLauncher(ConfigurationService configurationService) {
        if (configurationService == null) {
            throw new NullPointerException("ConfigurationService cannot be null");
        }
        this.configurationService = configurationService;
    }

    /**
     * Launches the text AI (Ollama) service process.
     *
     * @return a ProcessWrapper wrapping the launched process
     * @throws IOException if the process cannot be started
     */
    @Override
    public ProcessWrapper launchTextAI() throws IOException {
        String command = configurationService.getOllamaCommand();
        logger.info("Launching text AI service: {}", command);

        List<String> commandParts = parseCommand(command);
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);

        // Redirect error stream to avoid blocking
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            logger.info("Text AI service process started with PID: {}", process.pid());
            return new DefaultProcessWrapper(process);
        } catch (IOException e) {
            logger.error("Failed to launch text AI service: {}", command, e);
            throw e;
        }
    }

    /**
     * Launches the image AI (Stable Diffusion) service process.
     *
     * @return a ProcessWrapper wrapping the launched process
     * @throws IOException if the process cannot be started
     */
    @Override
    public ProcessWrapper launchImageAI() throws IOException {
        String command = configurationService.getSdCommand();
        logger.info("Launching image AI service: {}", command);

        List<String> commandParts = parseCommand(command);
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);

        // Redirect error stream to avoid blocking
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            logger.info("Image AI service process started with PID: {}", process.pid());
            return new DefaultProcessWrapper(process);
        } catch (IOException e) {
            logger.error("Failed to launch image AI service: {}", command, e);
            throw e;
        }
    }

    /**
     * Parses a command string into a list of command parts.
     * Handles quoted strings and spaces in paths (critical for Windows paths like "C:\Program Files\...").
     *
     * @param command the command string to parse
     * @return list of command parts
     */
    private List<String> parseCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : command.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }
}
