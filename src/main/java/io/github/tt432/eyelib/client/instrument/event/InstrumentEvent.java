package io.github.tt432.eyelib.client.instrument.event;

import org.jspecify.annotations.Nullable;

/**
 * An instrument event record for hot-path zero-allocation event recording.
 * <p>
 * Events carry a type, source, metric name/value/unit, and an optional JSON payload
 * for complex data.  This is the sole allocation on the offer path — no further
 * wrapping objects are created inside {@link EventRingBuffer}.
 *
 * @param eventType   the category of the event (e.g. {@code "cache_size"},
 *                    {@code "jvm_memory"}, {@code "gc_activity"}, {@code "particle_count"})
 * @param source      the component that produced the event (e.g.
 *                    {@code "MolangCompileCache"}, {@code "RenderHelper"},
 *                    {@code "BrParticleRenderManager"})
 * @param metricName  the name of the measured value (e.g. {@code "heap_used_mb"},
 *                    {@code "cache_entries"}, {@code "emitter_count"})
 * @param metricValue the numeric value of the metric
 * @param metricUnit  the unit of the metric value (e.g. {@code "MB"}, {@code "count"},
 *                    {@code "files"}, {@code "ms"})
 * @param extraJson   optional JSON payload for complex or structured data; may be
 *                    {@code null}
 */
public record InstrumentEvent(
        String eventType,
        String source,
        String metricName,
        double metricValue,
        String metricUnit,
        @Nullable String extraJson
) {
    /**
     * Canonical constructor with implicit null-check via {@link java.util.Objects#requireNonNull}
     * on all non-nullable fields.
     */
    public InstrumentEvent {
        java.util.Objects.requireNonNull(eventType);
        java.util.Objects.requireNonNull(source);
        java.util.Objects.requireNonNull(metricName);
        java.util.Objects.requireNonNull(metricUnit);
    }
}
