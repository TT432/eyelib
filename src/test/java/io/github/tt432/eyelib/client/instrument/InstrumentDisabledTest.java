package io.github.tt432.eyelib.client.instrument;

import io.github.tt432.eyelib.client.instrument.db.BackgroundFlushService;
import io.github.tt432.eyelib.client.instrument.event.EventRingBuffer;
import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying that when instrumentation is disabled, there is zero
 * overhead: no DB file created, no unwanted side effects, no exceptions.
 * <p>
 * Since {@link InstrumentConfig#isEnabled()} caches its value in a
 * {@code static final} field at class load time, setting the system
 * property in {@link #setUp()} does not change the already-loaded value.
 * The purpose of the property setup here is documentation and consistency
 * with {@link InstrumentConfigTest}. All assertions hold regardless
 * because the default value is {@code false}.
 * <p>
 * Tests that interact with {@link BackgroundFlushService} (install/shutdown)
 * leave the service in a non-installed state after tear-down to avoid
 * leaking scheduler threads across test cases.
 */
class InstrumentDisabledTest {

    @BeforeEach
    void setUp() {
        System.setProperty("eyelib.instrument.enabled", "false");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("eyelib.instrument.enabled");
        // Ensure BackgroundFlushService is shut down after tests that install it
        BackgroundFlushService.getInstance().shutdown();
        // Clean up any accidental DB files
        try {
            Files.deleteIfExists(Path.of("eyelib_instrument.mv.db"));
        } catch (final Exception ignored) {
            // best-effort cleanup
        }
        try {
            Files.deleteIfExists(Path.of("eyelib_instrument.trace.db"));
        } catch (final Exception ignored) {
            // best-effort cleanup
        }
    }

    // ---------------------------------------------------------------
    // InstrumentConfig
    // ---------------------------------------------------------------

    @Test
    void disabledConfigReturnsFalse() {
        assertFalse(InstrumentConfig.isEnabled(),
                "InstrumentConfig should be disabled by default");
    }

    // ---------------------------------------------------------------
    // EventRingBuffer
    // ---------------------------------------------------------------

    @Test
    void eventRingBufferStartsEmpty() {
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        assertTrue(buffer.isEmpty(), "Ring buffer should start empty");
        assertEquals(0, buffer.size(), "Ring buffer should have size 0");
    }

    @Test
    void eventRingBufferAcceptsEventsEvenWhenDisabled() {
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        // Drain any events left by previous tests
        while (buffer.poll() != null) {
            // drain
        }

        assertDoesNotThrow(() -> buffer.offer(
                new InstrumentEvent("test", "test", "test", 1.0, "count", null)));
        assertFalse(buffer.isEmpty(), "Ring buffer should contain the offered event");
        assertEquals(1, buffer.size(), "Ring buffer should have size 1 after one offer");

        // Clean up
        buffer.poll();
    }

    @Test
    void eventRingBufferPollReturnsNullWhenEmpty() {
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        // Drain any events left by previous tests
        while (buffer.poll() != null) {
            // drain
        }

        assertNull(buffer.poll(), "Poll on empty buffer should return null");
    }

    // ---------------------------------------------------------------
    // BackgroundFlushService
    // ---------------------------------------------------------------

    @Test
    void backgroundFlushServiceInstallIsSafeWhenDisabled() {
        assertDoesNotThrow(() -> BackgroundFlushService.getInstance().install(),
                "install() should not throw even when instrumentation is disabled");
    }

    @Test
    void backgroundFlushServiceShutdownIsSafeWhenDisabled() {
        assertDoesNotThrow(() -> BackgroundFlushService.getInstance().shutdown(),
                "shutdown() should not throw even when instrumentation is disabled");
    }

    @Test
    void backgroundFlushServiceInstallIsIdempotent() {
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        assertDoesNotThrow(() -> {
            service.install();
            service.install(); // second call should be a no-op
        }, "install() should be idempotent");
    }

    @Test
    void backgroundFlushServiceShutdownIsIdempotent() {
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        assertDoesNotThrow(() -> {
            service.shutdown();
            service.shutdown(); // second call should be a no-op
        }, "shutdown() should be idempotent");
    }

    @Test
    void backgroundFlushServiceFullLifecycleIsSafe() {
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        assertDoesNotThrow(() -> {
            service.install();
            // Let one flush cycle run (or at least not throw)
            Thread.sleep(100);
            service.shutdown();
        }, "Full install-flush-shutdown lifecycle should complete without error");
    }

    // ---------------------------------------------------------------
    // Zero-overhead: no DB files created when disabled
    // ---------------------------------------------------------------

    @Test
    void noDatabaseFilesCreated() {
        // Touch the singletons to trigger lazy initialization
        EventRingBuffer.getInstance();
        BackgroundFlushService.getInstance();

        // Neither EventRingBuffer nor BackgroundFlushService alone should
        // create database files.  Only calling database.getConnection()
        // would trigger that.
        assertTrue(
                Files.notExists(Path.of("eyelib_instrument.mv.db"))
                        && Files.notExists(Path.of("eyelib_instrument.trace.db")),
                "No H2 database files should exist when instrumentation is disabled " +
                        "and getConnection() has never been called");
    }
}
