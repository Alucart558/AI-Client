package com.aiclient.adapter.output.process;

import com.aiclient.domain.port.output.ProcessManagementPort;
import com.aiclient.domain.port.output.ProcessManagementPort.ProcessStartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ProcessManagementAdapter.
 * Tests lifecycle management of external AI service processes (Ollama and Stable Diffusion).
 * Uses JUnit 5, Mockito 5.10, and AssertJ 3.25.3 with mockable interfaces to avoid
 * mocking Java's sealed classes (Process, ProcessBuilder).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessManagementAdapter Test Suite")
class ProcessManagementAdapterTest {

    private ProcessManagementPort processManagementAdapter;

    @Mock
    private ProcessWrapper mockTextAIProcess;

    @Mock
    private ProcessWrapper mockImageAIProcess;

    @Mock
    private ProcessLauncher mockProcessLauncher;

    @BeforeEach
    void setUp() {
        // Initialize the adapter with mocked process launcher
        processManagementAdapter = new ProcessManagementAdapter(mockProcessLauncher);
    }

    // ==================== START TEXT AI SERVICE TESTS ====================

    @Test
    @DisplayName("Should start text AI service successfully")
    void testStartTextAIService_Success() throws Exception {
        // GIVEN - mocked process launcher returns mock ProcessWrapper that simulates successful startup
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        // Explicitly mock waitFor to avoid treating unmocked false as timeout
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // WHEN - start the text AI service
        processManagementAdapter.startTextAIService();

        // THEN - verify process was started
        verify(mockProcessLauncher).launchTextAI();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isTrue();
    }

    @Test
    @DisplayName("Should throw ProcessStartException when text AI service command not found")
    void testStartTextAIService_ProcessNotFound() throws Exception {
        // GIVEN - Process launcher throws IOException (simulating command not found)
        when(mockProcessLauncher.launchTextAI()).thenThrow(new java.io.IOException("Command not found"));

        // WHEN/THEN - attempting to start should throw ProcessStartException
        assertThatThrownBy(() -> processManagementAdapter.startTextAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("Failed to start text AI service")
            .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("Should throw ProcessStartException when text AI service startup timeout (crash detected)")
    void testStartTextAIService_Timeout() throws Exception {
        // GIVEN - process starts but crashes before ready (timeout scenario)
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive())
            .thenReturn(true)   // Initially alive
            .thenReturn(false); // Dies during waitFor
        when(mockTextAIProcess.waitFor(eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);
        when(mockTextAIProcess.exitValue()).thenReturn(1);

        // WHEN/THEN - should throw ProcessStartException due to crash
        assertThatThrownBy(() -> processManagementAdapter.startTextAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("terminated");
    }

    @Test
    @DisplayName("Should handle idempotent text AI service start (already running)")
    void testStartTextAIService_AlreadyRunning() throws Exception {
        // GIVEN - text AI service is already running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);

        // WHEN - start the service
        processManagementAdapter.startTextAIService();

        // AND - attempt to start again without stopping
        // THEN - should be idempotent and not throw exception or start a second process
        processManagementAdapter.startTextAIService();
        verify(mockProcessLauncher, times(1)).launchTextAI();
    }

    @Test
    @DisplayName("Should throw ProcessStartException when text AI process crashes after start")
    void testStartTextAIService_ProcessCrashesAfterStart() throws Exception {
        // GIVEN - process starts successfully but crashes immediately
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive())
            .thenReturn(true)  // Initially alive
            .thenReturn(false); // Then dies before ready check
        when(mockTextAIProcess.exitValue()).thenReturn(1);

        // WHEN/THEN - should detect crash and throw exception
        assertThatThrownBy(() -> processManagementAdapter.startTextAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("process terminated");
    }

    // ==================== START IMAGE AI SERVICE TESTS ====================

    @Test
    @DisplayName("Should start image AI service successfully")
    void testStartImageAIService_Success() throws Exception {
        // GIVEN - mocked process launcher returns mock ProcessWrapper that simulates successful startup
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        // Explicitly mock waitFor to avoid treating unmocked false as timeout
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // WHEN - start the image AI service
        processManagementAdapter.startImageAIService();

        // THEN - verify process was started
        verify(mockProcessLauncher).launchImageAI();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isTrue();
    }

    @Test
    @DisplayName("Should throw ProcessStartException when image AI service command not found")
    void testStartImageAIService_ProcessNotFound() throws Exception {
        // GIVEN - Process launcher throws IOException (simulating command not found)
        when(mockProcessLauncher.launchImageAI()).thenThrow(new java.io.IOException("Command not found"));

        // WHEN/THEN - attempting to start should throw ProcessStartException
        assertThatThrownBy(() -> processManagementAdapter.startImageAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("Failed to start image AI service")
            .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("Should throw ProcessStartException when image AI service startup timeout (crash detected)")
    void testStartImageAIService_Timeout() throws Exception {
        // GIVEN - process starts but crashes before ready (timeout scenario)
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive())
            .thenReturn(true)   // Initially alive
            .thenReturn(false); // Dies during waitFor
        when(mockImageAIProcess.waitFor(eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);
        when(mockImageAIProcess.exitValue()).thenReturn(1);

        // WHEN/THEN - should throw ProcessStartException due to crash
        assertThatThrownBy(() -> processManagementAdapter.startImageAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("terminated");
    }

    @Test
    @DisplayName("Should handle idempotent image AI service start (already running)")
    void testStartImageAIService_AlreadyRunning() throws Exception {
        // GIVEN - image AI service is already running
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);

        // WHEN - start the service
        processManagementAdapter.startImageAIService();

        // AND - attempt to start again without stopping
        // THEN - should be idempotent and not throw exception or start a second process
        processManagementAdapter.startImageAIService();
        verify(mockProcessLauncher, times(1)).launchImageAI();
    }

    @Test
    @DisplayName("Should handle image AI process crash after successful start")
    void testStartImageAIService_ProcessCrashesAfterStart() throws Exception {
        // GIVEN - process starts successfully but crashes immediately
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive())
            .thenReturn(true)  // Initially alive
            .thenReturn(false); // Then dies before ready check
        when(mockImageAIProcess.exitValue()).thenReturn(1);

        // WHEN/THEN - should detect crash and throw exception
        assertThatThrownBy(() -> processManagementAdapter.startImageAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("process terminated");
    }

    // ==================== IS TEXT AI SERVICE RUNNING TESTS ====================

    @Test
    @DisplayName("Should return true when text AI service is running")
    void testIsTextAIServiceRunning_WhenRunning() throws Exception {
        // GIVEN - text AI service has been started successfully
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();

        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isTextAIServiceRunning();

        // THEN - should return true
        assertThat(isRunning).isTrue();
        verify(mockTextAIProcess, atLeastOnce()).isAlive();
    }

    @Test
    @DisplayName("Should return false when text AI service is not running")
    void testIsTextAIServiceRunning_WhenStopped() {
        // GIVEN - text AI service has not been started
        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isTextAIServiceRunning();

        // THEN - should return false
        assertThat(isRunning).isFalse();
    }

    @Test
    @DisplayName("Should return false when text AI service process is dead")
    void testIsTextAIServiceRunning_WhenProcessDead() throws Exception {
        // GIVEN - text AI service was started
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();

        // AND - process dies
        when(mockTextAIProcess.isAlive()).thenReturn(false);

        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isTextAIServiceRunning();

        // THEN - should return false
        assertThat(isRunning).isFalse();
    }

    // ==================== IS IMAGE AI SERVICE RUNNING TESTS ====================

    @Test
    @DisplayName("Should return true when image AI service is running")
    void testIsImageAIServiceRunning_WhenRunning() throws Exception {
        // GIVEN - image AI service has been started successfully
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startImageAIService();

        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isImageAIServiceRunning();

        // THEN - should return true
        assertThat(isRunning).isTrue();
        verify(mockImageAIProcess, atLeastOnce()).isAlive();
    }

    @Test
    @DisplayName("Should return false when image AI service is not running")
    void testIsImageAIServiceRunning_WhenStopped() {
        // GIVEN - image AI service has not been started
        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isImageAIServiceRunning();

        // THEN - should return false
        assertThat(isRunning).isFalse();
    }

    @Test
    @DisplayName("Should return false when image AI service process is dead")
    void testIsImageAIServiceRunning_WhenProcessDead() throws Exception {
        // GIVEN - image AI service was started
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startImageAIService();

        // AND - process dies
        when(mockImageAIProcess.isAlive()).thenReturn(false);

        // WHEN - check if service is running
        boolean isRunning = processManagementAdapter.isImageAIServiceRunning();

        // THEN - should return false
        assertThat(isRunning).isFalse();
    }

    // ==================== STOP ALL SERVICES TESTS ====================

    @Test
    @DisplayName("Should stop all services gracefully when both are running")
    void testStopAllServices_GracefulShutdown() throws Exception {
        // GIVEN - both services are running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();
        processManagementAdapter.startImageAIService();

        // AND - graceful shutdown succeeds
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockTextAIProcess.isAlive()).thenReturn(false);
        when(mockImageAIProcess.isAlive()).thenReturn(false);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // THEN - verify both processes were destroyed gracefully
        verify(mockTextAIProcess).destroy();
        verify(mockImageAIProcess).destroy();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isFalse();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isFalse();
    }

    @Test
    @DisplayName("Should force kill services when graceful shutdown fails")
    void testStopAllServices_ForcedKill() throws Exception {
        // GIVEN - both services are running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();
        processManagementAdapter.startImageAIService();

        // AND - graceful shutdown fails, process still alive
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(true);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // THEN - verify force kill was called
        verify(mockTextAIProcess).destroyForcibly();
        verify(mockImageAIProcess).destroyForcibly();
    }

    @Test
    @DisplayName("Should handle stop gracefully when only text AI service is running")
    void testStopAllServices_OnlyTextAIRunning() throws Exception {
        // GIVEN - only text AI service is running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();

        // AND - graceful shutdown succeeds
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockTextAIProcess.isAlive()).thenReturn(false);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // THEN - verify text AI process was destroyed
        verify(mockTextAIProcess).destroy();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isFalse();
    }

    @Test
    @DisplayName("Should handle stop gracefully when only image AI service is running")
    void testStopAllServices_OnlyImageAIRunning() throws Exception {
        // GIVEN - only image AI service is running
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startImageAIService();

        // AND - graceful shutdown succeeds
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(false);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // THEN - verify image AI process was destroyed
        verify(mockImageAIProcess).destroy();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isFalse();
    }

    @Test
    @DisplayName("Should handle stopAllServices when no processes have been started")
    void testStopAllServices_NoProcessesStarted() {
        // GIVEN - no services have been started
        // WHEN - stop all services
        // THEN - should not throw exception
        assertThatCode(() -> processManagementAdapter.stopAllServices())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should make multiple stopAllServices calls idempotent")
    void testStopAllServices_MultipleCallsIdempotent() throws Exception {
        // GIVEN - both services are running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();
        processManagementAdapter.startImageAIService();

        // AND - graceful shutdown succeeds
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockTextAIProcess.isAlive()).thenReturn(false);
        when(mockImageAIProcess.isAlive()).thenReturn(false);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // AND - call stop again
        // THEN - should not throw exception and not attempt to destroy again
        assertThatCode(() -> processManagementAdapter.stopAllServices())
            .doesNotThrowAnyException();
        // Verify destroy was only called once per process
        verify(mockTextAIProcess, times(1)).destroy();
        verify(mockImageAIProcess, times(1)).destroy();
    }

    @Test
    @DisplayName("Should handle force kill when graceful shutdown times out for text AI")
    void testStopAllServices_ForcedKillTextAIOnly() throws Exception {
        // GIVEN - text AI service is running
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        processManagementAdapter.startTextAIService();

        // AND - graceful shutdown fails
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(mockTextAIProcess.isAlive()).thenReturn(true).thenReturn(false);

        // WHEN - stop all services
        processManagementAdapter.stopAllServices();

        // THEN - verify destroy and destroyForcibly were called in sequence
        verify(mockTextAIProcess).destroy();
        verify(mockTextAIProcess).destroyForcibly();
    }

    // ==================== CONCURRENT OPERATIONS TESTS ====================

    @Test
    @DisplayName("Should handle concurrent start attempts on text AI service")
    void testConcurrentStartAttempts_TextAI() throws Exception {
        // GIVEN - mocked process launcher for text AI
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);

        // WHEN - first thread starts service
        processManagementAdapter.startTextAIService();

        // AND - second attempt to start (should be idempotent)
        processManagementAdapter.startTextAIService();

        // THEN - process should only be started once
        verify(mockProcessLauncher, times(1)).launchTextAI();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isTrue();
    }

    @Test
    @DisplayName("Should handle concurrent start attempts on image AI service")
    void testConcurrentStartAttempts_ImageAI() throws Exception {
        // GIVEN - mocked process launcher for image AI
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockImageAIProcess.isAlive()).thenReturn(true);

        // WHEN - first thread starts service
        processManagementAdapter.startImageAIService();

        // AND - second attempt to start (should be idempotent)
        processManagementAdapter.startImageAIService();

        // THEN - process should only be started once
        verify(mockProcessLauncher, times(1)).launchImageAI();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isTrue();
    }

    // ==================== CONFIGURATION AND EDGE CASES ====================

    @Test
    @DisplayName("Should use correct timeout configuration for process startup")
    void testStartupTimeoutConfiguration() throws Exception {
        // GIVEN - mocked process launcher and timeout configuration
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.waitFor(eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(mockTextAIProcess.isAlive()).thenReturn(true);

        // WHEN - start text AI service
        processManagementAdapter.startTextAIService();

        // THEN - verify timeout was applied correctly (30 seconds)
        verify(mockTextAIProcess).waitFor(30L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should handle system interruption during process start")
    void testStartTextAIService_InterruptedException() throws Exception {
        // GIVEN - Process launcher succeeds but waitFor throws InterruptedException
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true); // Process is alive initially
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class)))
            .thenThrow(new InterruptedException("Thread interrupted"));

        // WHEN/THEN - should throw ProcessStartException wrapping the InterruptedException
        assertThatThrownBy(() -> processManagementAdapter.startTextAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    @DisplayName("Should handle process exit code during startup")
    void testStartTextAIService_ExitCodeError() throws Exception {
        // GIVEN - process starts alive but terminates with error code
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockTextAIProcess.isAlive())
            .thenReturn(true)   // Initially alive
            .thenReturn(false); // Dies after waitFor detects termination
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockTextAIProcess.exitValue()).thenReturn(127); // Command not found

        // WHEN/THEN - should throw ProcessStartException
        assertThatThrownBy(() -> processManagementAdapter.startTextAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("exit code");
    }

    @Test
    @DisplayName("Should safely handle null process reference")
    void testStopAllServices_WithNullProcessReference() {
        // GIVEN - adapter state with potential null references
        // WHEN - stop all services
        // THEN - should handle gracefully without throwing NullPointerException
        assertThatCode(() -> processManagementAdapter.stopAllServices())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should verify independent lifecycle of text and image services")
    void testIndependentServiceLifecycle() throws Exception {
        // GIVEN - both services configured with different process mocks
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockImageAIProcess.isAlive()).thenReturn(true);

        // WHEN - start text AI service
        processManagementAdapter.startTextAIService();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isTrue();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isFalse();

        // AND - start image AI service
        processManagementAdapter.startImageAIService();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isTrue();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isTrue();

        // AND - stop all services
        when(mockTextAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockImageAIProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockTextAIProcess.isAlive()).thenReturn(false);
        when(mockImageAIProcess.isAlive()).thenReturn(false);
        processManagementAdapter.stopAllServices();

        // THEN - verify both are stopped (stopAllServices stops all)
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isFalse();
        assertThat(processManagementAdapter.isImageAIServiceRunning()).isFalse();
    }

    @Test
    @DisplayName("Should handle image AI crash separately from text AI success")
    void testTimeoutHandling_Independent() throws Exception {
        // GIVEN - text AI succeeds but image AI crashes
        when(mockProcessLauncher.launchTextAI()).thenReturn(mockTextAIProcess);
        when(mockProcessLauncher.launchImageAI()).thenReturn(mockImageAIProcess);
        // Text AI: stays alive
        when(mockTextAIProcess.isAlive()).thenReturn(true);
        when(mockTextAIProcess.waitFor(eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);
        // Image AI: crashes during startup
        when(mockImageAIProcess.isAlive())
            .thenReturn(true)   // Initially alive
            .thenReturn(false); // Dies during waitFor
        when(mockImageAIProcess.waitFor(eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);
        when(mockImageAIProcess.exitValue()).thenReturn(1);

        // WHEN - start text AI (should succeed)
        processManagementAdapter.startTextAIService();
        assertThat(processManagementAdapter.isTextAIServiceRunning()).isTrue();

        // AND - start image AI (should fail due to crash)
        assertThatThrownBy(() -> processManagementAdapter.startImageAIService())
            .isInstanceOf(ProcessStartException.class)
            .hasMessageContaining("terminated");
    }
}
