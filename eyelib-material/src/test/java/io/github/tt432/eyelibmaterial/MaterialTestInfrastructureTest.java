package io.github.tt432.eyelibmaterial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test to verify JUnit 5 test infrastructure works for the eyelib-material subproject.
 */
class MaterialTestInfrastructureTest {

    @Test
    void infrastructureWorks() {
        assertTrue(true);
    }

    @Test
    void basicAssertions() {
        assertEquals(4, 2 + 2);
    }
}
