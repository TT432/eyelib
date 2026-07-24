package io.github.tt432.eyelib.debug.benchmark;

import org.jspecify.annotations.Nullable;

/** Complete in-memory result handed from the runner to the report writer. */
record BenchmarkResult(
        BenchmarkScenario scenario,
        FrameSampleBuffer frames,
        ResourceSampleBuffer resources,
        BenchmarkStatistics.Summary summary,
        String status,
        @Nullable String error,
        int configuredEntityCount,
        int actualEntityCount,
        int eyelibRenderErrorCount,
        @Nullable String eyelibLastRenderError
) {
}
