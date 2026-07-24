package io.github.tt432.eyelib.debug.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BenchmarkPhaseTest {
    @Test
    void phaseCodesAreCompactStableValues() {
        assertEquals((byte) 0, BenchmarkPhase.WARMUP.code());
        assertEquals((byte) 1, BenchmarkPhase.MEASURE.code());
        assertEquals(BenchmarkPhase.WARMUP, BenchmarkPhase.fromCode((byte) 0));
        assertEquals(BenchmarkPhase.MEASURE, BenchmarkPhase.fromCode((byte) 1));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkPhase.fromCode((byte) 2));
    }
}
