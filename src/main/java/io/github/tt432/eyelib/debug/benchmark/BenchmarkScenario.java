package io.github.tt432.eyelib.debug.benchmark;

import java.util.List;

/**
 * Immutable description of one benchmark workload.
 *
 * @param id stable report identifier
 * @param kind rendering path under test
 * @param purpose benchmark question answered by the scenario
 * @param entitySpecs ordered entity types and counts used by the workload
 * @param targetFps zero for uncapped throughput, otherwise the pacing target
 * @param fboSize off-screen target edge length; ignored by world scenarios
 * @param warmupSeconds warmup duration retained in raw output
 * @param measureSeconds measured duration
 */
public record BenchmarkScenario(
        String id,
        Kind kind,
        Purpose purpose,
        List<EntitySpec> entitySpecs,
        int targetFps,
        int fboSize,
        int warmupSeconds,
        int measureSeconds
) {
    public BenchmarkScenario {
        entitySpecs = List.copyOf(entitySpecs);
        if (entitySpecs.isEmpty()) {
            throw new IllegalArgumentException("A benchmark scenario requires at least one entity type");
        }
        int totalCount = entitySpecs.stream().mapToInt(EntitySpec::count).sum();
        if (totalCount <= 0) {
            throw new IllegalArgumentException("A benchmark scenario requires at least one entity");
        }
    }

    public int entityCount() {
        return entitySpecs.stream().mapToInt(EntitySpec::count).sum();
    }

    /** Stable human-readable composition retained in metadata and logs. */
    public String entityComposition() {
        return entitySpecs.stream()
                .map(spec -> spec.id() + "=" + spec.count())
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
    }

    public String entityId() {
        return entitySpecs.size() == 1 ? entitySpecs.get(0).id() : "mixed";
    }

    public enum Kind {
        FBO,
        WORLD
    }

    public enum Purpose {
        THROUGHPUT,
        PACING,
        STABILITY
    }

    public record EntitySpec(String id, int count) {
        public EntitySpec {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Entity type id must not be blank");
            }
            if (count <= 0) {
                throw new IllegalArgumentException("Entity count must be positive: " + count);
            }
        }
    }
}
