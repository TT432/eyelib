package io.github.tt432.eyelib.debug.benchmark;

/**
 * Immutable description of one benchmark workload.
 *
 * @param id stable report identifier
 * @param kind rendering path under test
 * @param purpose benchmark question answered by the scenario
 * @param entityId Minecraft entity registry id
 * @param entityCount fixed number of entities rendered per frame
 * @param targetFps zero for uncapped throughput, otherwise the pacing target
 * @param fboSize off-screen target edge length; ignored by world scenarios
 * @param warmupSeconds warmup duration retained in raw output
 * @param measureSeconds measured duration
 */
public record BenchmarkScenario(
        String id,
        Kind kind,
        Purpose purpose,
        String entityId,
        int entityCount,
        int targetFps,
        int fboSize,
        int warmupSeconds,
        int measureSeconds
) {
    public enum Kind {
        FBO,
        WORLD
    }

    public enum Purpose {
        THROUGHPUT,
        PACING,
        STABILITY
    }
}
