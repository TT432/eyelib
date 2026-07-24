package io.github.tt432.eyelib.debug.benchmark;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** System-property backed configuration for the dev-only benchmark runner. */
public final class ClientBenchmarkConfig {
    public static final String ENABLED_PROPERTY = "eyelib.benchmark.enabled";

    private ClientBenchmarkConfig() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean(ENABLED_PROPERTY);
    }

    public static boolean shouldAutoExit() {
        return boolProperty("eyelib.benchmark.autoExit", true);
    }

    public static String worldName() {
        return stringProperty("eyelib.benchmark.worldName", "EyelibBenchmark");
    }

    public static int stabilizeTicks() {
        return intProperty("eyelib.benchmark.stabilizeTicks", 100, 0, 2400);
    }

    public static int maxSamples() {
        return intProperty("eyelib.benchmark.maxSamples", 2_000_000, 1_000, 20_000_000);
    }

    public static List<BenchmarkScenario> scenarios() {
        String entityId = stringProperty("eyelib.benchmark.entity", "minecraft:slime");
        int[] counts = intListProperty("eyelib.benchmark.counts", "1,16,64", 1, 4096);
        Set<String> modes = stringSetProperty("eyelib.benchmark.modes", "fbo,world,pacing,stability");
        List<BenchmarkScenario.EntitySpec> mixedEntities = modes.contains("mixed")
                ? entityMixProperty(
                "eyelib.benchmark.entityMix",
                "minecraft:slime=24,minecraft:zombie=24,minecraft:skeleton=24,minecraft:cow=24"
        ) : List.of();
        int fboSize = intProperty("eyelib.benchmark.fboSize", 512, 64, 4096);
        int warmup = intProperty("eyelib.benchmark.warmupSeconds", 15, 0, 3600);
        int measure = intProperty("eyelib.benchmark.measureSeconds", 30, 1, 7200);
        int pacingCount = intProperty("eyelib.benchmark.pacingCount", 16, 1, 4096);
        int stabilityCount = intProperty("eyelib.benchmark.stabilityCount", 64, 1, 4096);
        int stabilityWarmup = intProperty("eyelib.benchmark.stabilityWarmupSeconds", 30, 0, 3600);
        int stabilitySeconds = intProperty("eyelib.benchmark.stabilitySeconds", 600, 1, 7200);

        List<BenchmarkScenario> result = new ArrayList<>();
        List<BenchmarkScenario.EntitySpec> singleEntity = List.of(new BenchmarkScenario.EntitySpec(entityId, 1));
        if (modes.contains("fbo")) {
            for (int count : counts) {
                result.add(scenario(
                        "fbo-uncapped-n" + count,
                        BenchmarkScenario.Kind.FBO,
                        BenchmarkScenario.Purpose.THROUGHPUT,
                        scale(singleEntity, count),
                        0,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("world")) {
            for (int count : counts) {
                result.add(scenario(
                        "world-uncapped-n" + count,
                        BenchmarkScenario.Kind.WORLD,
                        BenchmarkScenario.Purpose.THROUGHPUT,
                        scale(singleEntity, count),
                        0,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("pacing")) {
            for (int fps : new int[]{30, 60, 120}) {
                result.add(scenario(
                        "world-pacing-" + fps + "fps-n" + pacingCount,
                        BenchmarkScenario.Kind.WORLD,
                        BenchmarkScenario.Purpose.PACING,
                        scale(singleEntity, pacingCount),
                        fps,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("stability")) {
            result.add(scenario(
                    "world-stability-n" + stabilityCount,
                    BenchmarkScenario.Kind.WORLD,
                    BenchmarkScenario.Purpose.STABILITY,
                    scale(singleEntity, stabilityCount),
                    0,
                    fboSize,
                    stabilityWarmup,
                    stabilitySeconds
            ));
        }
        if (modes.contains("mixed")) {
            int mixedCount = mixedEntities.stream().mapToInt(BenchmarkScenario.EntitySpec::count).sum();
            result.add(scenario(
                    "fbo-mixed-n" + mixedCount,
                    BenchmarkScenario.Kind.FBO,
                    BenchmarkScenario.Purpose.THROUGHPUT,
                    mixedEntities,
                    0,
                    fboSize,
                    warmup,
                    measure
            ));
            result.add(scenario(
                    "world-mixed-n" + mixedCount,
                    BenchmarkScenario.Kind.WORLD,
                    BenchmarkScenario.Purpose.THROUGHPUT,
                    mixedEntities,
                    0,
                    fboSize,
                    warmup,
                    measure
            ));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("eyelib.benchmark.modes selected no scenarios: " + modes);
        }
        return List.copyOf(result);
    }

    private static BenchmarkScenario scenario(
            String id,
            BenchmarkScenario.Kind kind,
            BenchmarkScenario.Purpose purpose,
            List<BenchmarkScenario.EntitySpec> entitySpecs,
            int targetFps,
            int fboSize,
            int warmup,
            int measure
    ) {
        return new BenchmarkScenario(id, kind, purpose, entitySpecs, targetFps, fboSize, warmup, measure);
    }

    private static List<BenchmarkScenario.EntitySpec> scale(
            List<BenchmarkScenario.EntitySpec> entitySpecs,
            int totalCount
    ) {
        BenchmarkScenario.EntitySpec spec = entitySpecs.get(0);
        return List.of(new BenchmarkScenario.EntitySpec(spec.id(), totalCount));
    }

    private static List<BenchmarkScenario.EntitySpec> entityMixProperty(String key, String defaultValue) {
        String raw = stringProperty(key, defaultValue);
        List<BenchmarkScenario.EntitySpec> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        int total = 0;
        for (String part : raw.split(",")) {
            String[] assignment = part.trim().split("=", -1);
            if (assignment.length != 2 || assignment[0].isBlank() || assignment[1].isBlank()) {
                throw new IllegalArgumentException(key + " entries must use entity_id=count: " + part);
            }
            String id = assignment[0].trim();
            int count;
            try {
                count = Integer.parseInt(assignment[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " count is not an integer: " + part, e);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException(key + " contains duplicate entity type: " + id);
            }
            if (result.size() >= 16) {
                throw new IllegalArgumentException(key + " supports at most 16 entity types");
            }
            if (count < 1 || count > 4096) {
                throw new IllegalArgumentException(key + " count must be in [1, 4096], got " + count);
            }
            total += count;
            if (total > 4096) {
                throw new IllegalArgumentException(key + " total count must be in [1, 4096], got " + total);
            }
            result.add(new BenchmarkScenario.EntitySpec(id, count));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(key + " must contain at least one entity type");
        }
        return List.copyOf(result);
    }

    private static String stringProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static boolean boolProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static int intProperty(String key, int defaultValue, int min, int max) {
        String value = System.getProperty(key);
        int parsed = value == null ? defaultValue : Integer.parseInt(value.trim());
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(key + " must be in [" + min + ", " + max + "], got " + parsed);
        }
        return parsed;
    }

    private static int[] intListProperty(String key, String defaultValue, int min, int max) {
        String raw = stringProperty(key, defaultValue);
        String[] parts = raw.split(",");
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        for (String part : parts) {
            int value = Integer.parseInt(part.trim());
            if (value < min || value > max) {
                throw new IllegalArgumentException(key + " entry must be in [" + min + ", " + max + "], got " + value);
            }
            values.add(value);
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Set<String> stringSetProperty(String key, String defaultValue) {
        String raw = stringProperty(key, defaultValue);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String value = part.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
}
