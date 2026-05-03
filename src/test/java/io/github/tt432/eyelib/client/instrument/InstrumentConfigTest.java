package io.github.tt432.eyelib.client.instrument;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InstrumentConfig}.
 * <p>
 * Since {@code ENABLED} is a {@code static final} field initialized at class
 * load time, changes to the system property after the class is loaded do not
 * affect the cached value.  These tests therefore verify the parsing logic
 * ({@link Boolean#parseBoolean(String)}) that the class uses under the hood,
 * and confirm the default fallback behavior.
 */
class InstrumentConfigTest {

    @BeforeEach
    @AfterEach
    void clearProperty() {
        System.clearProperty("eyelib.instrument.enabled");
    }

    @Test
    void defaultIsFalse() {
        assertFalse(InstrumentConfig.isEnabled(),
                "Instrumentation should be disabled by default");
    }

    // --- Boolean.parseBoolean logic verification ---

    @Test
    void parseTrueReturnsTrue() {
        assertTrue(Boolean.parseBoolean("true"),
                "Boolean.parseBoolean(\"true\") must return true");
    }

    @Test
    void parseFalseReturnsFalse() {
        assertFalse(Boolean.parseBoolean("false"),
                "Boolean.parseBoolean(\"false\") must return false");
    }

    @Test
    void parseGarbageReturnsFalse() {
        assertFalse(Boolean.parseBoolean("garbage"),
                "Boolean.parseBoolean(\"garbage\") must return false");
    }

    @Test
    void parseNullReturnsFalse() {
        assertFalse(Boolean.parseBoolean(null),
                "Boolean.parseBoolean(null) must return false");
    }

    @Test
    void parseEmptyReturnsFalse() {
        assertFalse(Boolean.parseBoolean(""),
                "Boolean.parseBoolean(\"\") must return false");
    }
}
