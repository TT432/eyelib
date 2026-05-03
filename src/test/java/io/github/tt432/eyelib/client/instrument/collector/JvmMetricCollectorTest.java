package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmMetricCollectorTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void pollReturnsJvmMetricsWithPositiveHeapAndThreadValues() {
        JvmMetricCollector collector = new JvmMetricCollector();

        List<InstrumentEvent> events = collector.poll();

        assertNotNull(events, "poll() should return a non-null list");
        assertFalse(events.isEmpty(), "poll() should return at least one metric event");

        double heapUsedMb = metricValue(events, "heap_used_mb");
        double heapMaxMb = metricValue(events, "heap_max_mb");
        double threadCount = metricValue(events, "thread_count");

        assertTrue(heapUsedMb > EPSILON, "heap_used_mb should be > 0");
        assertTrue(heapMaxMb > EPSILON, "heap_max_mb should be > 0");
        assertTrue(threadCount > EPSILON, "thread_count should be > 0");
    }

    @Test
    void secondPollHasNonNegativeGcCollectionDelta() {
        JvmMetricCollector collector = new JvmMetricCollector();

        collector.poll();
        List<InstrumentEvent> second = collector.poll();

        double gcCollectionsDelta = metricValue(second, "gc_collections");
        assertTrue(gcCollectionsDelta >= -EPSILON,
                "gc_collections delta should not be negative on subsequent polls");
    }

    @Test
    void allExpectedMetricNamesPresent() {
        JvmMetricCollector collector = new JvmMetricCollector();

        List<InstrumentEvent> events = collector.poll();
        Set<String> names = metricNames(events);

        assertTrue(names.contains("heap_used_mb"), "should contain heap_used_mb");
        assertTrue(names.contains("heap_max_mb"), "should contain heap_max_mb");
        assertTrue(names.contains("gc_collections"), "should contain gc_collections");
        assertTrue(names.contains("gc_time_ms"), "should contain gc_time_ms");
        assertTrue(names.contains("thread_count"), "should contain thread_count");
        assertTrue(names.contains("daemon_thread_count"), "should contain daemon_thread_count");
    }

    @Test
    void heapMetricsArePositive() {
        JvmMetricCollector collector = new JvmMetricCollector();

        List<InstrumentEvent> events = collector.poll();

        assertTrue(metricValue(events, "heap_used_mb") > EPSILON,
                "heap_used_mb should be strictly positive");
        assertTrue(metricValue(events, "heap_max_mb") > EPSILON,
                "heap_max_mb should be strictly positive");
    }

    @Test
    void gcDeltaIsNonNegative() {
        JvmMetricCollector collector = new JvmMetricCollector();

        collector.poll();
        List<InstrumentEvent> second = collector.poll();

        double gcCollections = metricValue(second, "gc_collections");
        assertTrue(gcCollections >= -EPSILON,
                "gc_collections delta should be >= 0 (may be 0 if no GC occurred)");
    }

    @Test
    void secondPollThreadCountsArePositive() {
        JvmMetricCollector collector = new JvmMetricCollector();

        List<InstrumentEvent> first = collector.poll();
        List<InstrumentEvent> second = collector.poll();

        assertTrue(metricValue(first, "thread_count") > EPSILON,
                "thread_count should be > 0 in first poll");
        assertTrue(metricValue(first, "daemon_thread_count") > EPSILON,
                "daemon_thread_count should be > 0 in first poll");
        assertTrue(metricValue(second, "thread_count") > EPSILON,
                "thread_count should be > 0 in second poll");
        assertTrue(metricValue(second, "daemon_thread_count") > EPSILON,
                "daemon_thread_count should be > 0 in second poll");
    }

    @Test
    void noNegativeMetrics() {
        JvmMetricCollector collector = new JvmMetricCollector();

        List<InstrumentEvent> events = collector.poll();

        for (InstrumentEvent event : events) {
            assertTrue(event.metricValue() >= -EPSILON,
                    "metric '" + event.metricName() + "' should not be negative, but was "
                            + event.metricValue());
        }
    }

    private static double metricValue(List<InstrumentEvent> events, String metricName) {
        for (InstrumentEvent event : events) {
            if (metricName.equals(event.metricName())) {
                return event.metricValue();
            }
        }
        throw new AssertionError("Missing metric event: " + metricName);
    }

    private static Set<String> metricNames(List<InstrumentEvent> events) {
        return events.stream()
                .map(InstrumentEvent::metricName)
                .collect(Collectors.toSet());
    }
}
