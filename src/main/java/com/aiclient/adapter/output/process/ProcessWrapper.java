package com.aiclient.adapter.output.process;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper interface around Java's Process class for abstraction and testability.
 *
 * This interface allows mocking process operations in unit tests without spawning real processes.
 * Implementations delegate to actual Process instances in production code.
 */
public interface ProcessWrapper {
    /**
     * Tests whether the process is alive.
     *
     * @return true if the process is alive, false otherwise
     */
    boolean isAlive();

    /**
     * Kills the process gracefully.
     * The process may not terminate immediately.
     */
    void destroy();

    /**
     * Kills the process forcibly.
     * The process terminates immediately.
     */
    void destroyForcibly();

    /**
     * Returns the exit value of the process.
     *
     * @return the exit code of the process
     * @throws IllegalThreadStateException if the process has not yet terminated
     */
    int exitValue();

    /**
     * Waits for the process to terminate, up to the specified timeout.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout
     * @return true if the process terminated, false if the timeout elapsed
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;
}
