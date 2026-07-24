package io.github.tt432.eyelib.debug.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceSampleBufferTest {
    @Test
    void storesAllResourceFieldsAndPreservesThemOnOverflow() {
        ResourceSampleBuffer buffer = new ResourceSampleBuffer(1);

        assertTrue(buffer.record(10, 20, 30, 40, 50, 0.75));
        assertFalse(buffer.record(11, 21, 31, 41, 51, 0.25));

        assertEquals(1, buffer.size());
        assertEquals(1, buffer.capacity());
        assertTrue(buffer.overflow());
        assertEquals(10, buffer.timestampNs(0));
        assertEquals(20, buffer.heapUsedBytes(0));
        assertEquals(30, buffer.heapCommittedBytes(0));
        assertEquals(40, buffer.gcCount(0));
        assertEquals(50, buffer.gcPauseMs(0));
        assertEquals(0.75, buffer.processCpuLoad(0));
    }
}
