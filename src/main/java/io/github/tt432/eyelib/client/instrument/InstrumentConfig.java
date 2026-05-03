package io.github.tt432.eyelib.client.instrument;

import java.util.logging.Logger;

/**
 * Feature flag for eyelib instrumentation.
 * <p>
 * Reads the JVM property {@code eyelib.instrument.enabled} once at class
 * initialization time. The value is cached and never re-read, so changing
 * the system property at runtime has no effect after the class is loaded.
 * <p>
 * Usage:
 * <pre>{@code
 * if (InstrumentConfig.isEnabled()) {
 *     // emit instrumentation data
 * }
 * }</pre>
 */
public final class InstrumentConfig {
    private static final Logger LOG = Logger.getLogger(InstrumentConfig.class.getName());

    private static final boolean ENABLED;

    static {
        ENABLED = Boolean.parseBoolean(System.getProperty("eyelib.instrument.enabled", "false"));
        if (ENABLED) {
            LOG.info("Eyelib instrumentation enabled — writing to: eyelib_instrument");
        }
    }

    private InstrumentConfig() {
    }

    /**
     * Returns {@code true} if instrumentation is enabled, {@code false} otherwise.
     * <p>
     * The value is determined by the JVM property {@code eyelib.instrument.enabled}
     * and is cached at class initialization time.
     *
     * @return the cached instrumentation enabled flag
     */
    public static boolean isEnabled() {
        return ENABLED;
    }
}
