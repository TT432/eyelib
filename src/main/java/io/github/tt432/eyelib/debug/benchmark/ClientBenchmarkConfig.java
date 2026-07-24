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
        int fboSize = intProperty("eyelib.benchmark.fboSize", 512, 64, 4096);
        int warmup = intProperty("eyelib.benchmark.warmupSeconds", 15, 0, 3600);
        int measure = intProperty("eyelib.benchmark.measureSeconds", 30, 1, 7200);
        int pacingCount = intProperty("eyelib.benchmark.pacingCount", 16, 1, 4096);
        int stabilityCount = intProperty("eyelib.benchmark.stabilityCount", 64, 1, 4096);
        int stabilityWarmup = intProperty("eyelib.benchmark.stabilityWarmupSeconds", 30, 0, 3600);
        int stabilitySeconds = intProperty("eyelib.benchmark.stabilitySeconds", 600, 1, 7200);

        List<BenchmarkScenario> result = new ArrayList<>();
        if (modes.contains("fbo")) {
            for (int count : counts) {
                result.add(new BenchmarkScenario(
                        "fbo-uncapped-n" + count,
                        BenchmarkScenario.Kind.FBO,
                        BenchmarkScenario.Purpose.THROUGHPUT,
                        entityId,
                        count,
                        0,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("world")) {
            for (int count : counts) {
                result.add(new BenchmarkScenario(
                        "world-uncapped-n" + count,
                        BenchmarkScenario.Kind.WORLD,
                        BenchmarkScenario.Purpose.THROUGHPUT,
                        entityId,
                        count,
                        0,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("pacing")) {
            for (int fps : new int[]{30, 60, 120}) {
                result.add(new BenchmarkScenario(
                        "world-pacing-" + fps + "fps-n" + pacingCount,
                        BenchmarkScenario.Kind.WORLD,
                        BenchmarkScenario.Purpose.PACING,
                        entityId,
                        pacingCount,
                        fps,
                        fboSize,
                        warmup,
                        measure
                ));
            }
        }
        if (modes.contains("stability")) {
            result.add(new BenchmarkScenario(
                    "world-stability-n" + stabilityCount,
                    BenchmarkScenario.Kind.WORLD,
                    BenchmarkScenario.Purpose.STABILITY,
                    entityId,
                    stabilityCount,
                    0,
                    fboSize,
                    stabilityWarmup,
                    stabilitySeconds
            ));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("eyelib.benchmark.modes selected no scenarios: " + modes);
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
