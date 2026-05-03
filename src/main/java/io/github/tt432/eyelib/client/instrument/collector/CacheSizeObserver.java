package io.github.tt432.eyelib.client.instrument.collector;

/**
 * A functional observer that reports the current size of a cache or similar
 * resource pool for telemetry and instrumentation purposes.
 */
public interface CacheSizeObserver {
    /** Human-readable source name for telemetry identification. */
    String source();
    /** Short metric name, e.g. "cache_entries". */
    String metricName();
    /** Current size/count of the observed cache. */
    int currentSize();
    /** Unit of the metric, e.g. "count". */
    String metricUnit();
}
