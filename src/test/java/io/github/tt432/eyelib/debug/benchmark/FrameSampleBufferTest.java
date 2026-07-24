package io.github.tt432.eyelib.debug.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameSampleBufferTest {
    @Test
    void overflowRejectsNewSamplesWithoutOverwritingOldOnes() {
        FrameSampleBuffer buffer = new FrameSampleBuffer(2);

        assertTrue(buffer.record(10, 11, 12, 13, BenchmarkPhase.WARMUP));
        assertTrue(buffer.record(20, 21, 22, 23, BenchmarkPhase.MEASURE));
        assertFalse(buffer.record(30, 31, 32, 33, BenchmarkPhase.MEASURE));

        assertEquals(2, buffer.size());
        assertEquals(2, buffer.capacity());
        assertTrue(buffer.overflow());
        assertEquals(10, buffer.timestampNs(0));
        assertEquals(11, buffer.frameIntervalNs(0));
        assertEquals(12, buffer.renderWorkNs(0));
        assertEquals(13, buffer.renderedEntityCount(0));
        assertEquals(BenchmarkPhase.WARMUP, buffer.phase(0));
        assertEquals(20, buffer.timestampNs(1));
        assertEquals(21, buffer.frameIntervalNs(1));
        assertEquals(22, buffer.renderWorkNs(1));
        assertEquals(23, buffer.renderedEntityCount(1));
        assertEquals(BenchmarkPhase.MEASURE.code(), buffer.phaseCode(1));
    }

    @Test
    void zeroCapacityOverflowsOnFirstRecord() {
        FrameSampleBuffer buffer = new FrameSampleBuffer(0);

        assertFalse(buffer.record(1, 2, 3, FrameSampleBuffer.ENTITY_COUNT_UNAVAILABLE,
                BenchmarkPhase.MEASURE));
        assertEquals(0, buffer.size());
        assertTrue(buffer.overflow());
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.timestampNs(0));
    }
}
