package com.aiclient.adapter.output.process;

import java.util.concurrent.TimeUnit;

/**
 * Default implementation of ProcessWrapper that delegates to a real Process instance.
 * This wrapper provides a mockable interface around Java's Process class.
 *
 * <p>Process is a sealed class in Java, making it difficult to mock directly in tests.
 * This wrapper allows tests to inject mock implementations while production code
 * uses real Process instances.</p>
 */
public class DefaultProcessWrapper implements ProcessWrapper {

    private final Process process;

    /**
     * Creates a new wrapper around the given Process instance.
     *
     * @param process the Process to wrap
     * @throws NullPointerException if process is null
     */
    public DefaultProcessWrapper(Process process) {
        if (process == null) {
            throw new NullPointerException("Process cannot be null");
        }
        this.process = process;
    }

    /**
     * Tests whether the process is alive.
     *
     * @return true if the process has not yet terminated
     */
    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Kills the process. Whether the process is forcibly or gracefully terminated
     * is implementation dependent.
     */
    @Override
    public void destroy() {
        process.destroy();
    }

    /**
     * Kills the process forcibly. The process will be killed forcibly even if
     * it has not been destroyed yet.
     */
    @Override
    public void destroyForcibly() {
        process.destroyForcibly();
    }

    /**
     * Returns the exit value for the process.
     *
     * @return the exit value of the process
     * @throws IllegalThreadStateException if the process has not yet terminated
     */
    @Override
    public int exitValue() {
        return process.exitValue();
    }

    /**
     * Causes the current thread to wait, if necessary, until the process has
     * terminated or the timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if the process has exited, false if waiting time elapsed
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }
}
