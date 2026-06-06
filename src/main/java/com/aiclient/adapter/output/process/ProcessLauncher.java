package com.aiclient.adapter.output.process;

import java.io.IOException;

/**
 * Service interface for launching AI service processes.
 *
 * This abstraction allows the ProcessManagementAdapter to be tested
 * without spawning real processes. Implementations use ProcessBuilder
 * to create and start processes based on configuration.
 */
public interface ProcessLauncher {
    /**
     * Launches the text AI service process (Ollama).
     *
     * @return a wrapper around the launched process
     * @throws IOException if the process cannot be started
     */
    ProcessWrapper launchTextAI() throws IOException;

    /**
     * Launches the image AI service process (Stable Diffusion).
     *
     * @return a wrapper around the launched process
     * @throws IOException if the process cannot be started
     */
    ProcessWrapper launchImageAI() throws IOException;
}
