package io.github.tt432.eyelib.debug.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BenchmarkScenarioTest {
    @Test
    void mixedModeCreatesFboAndWorldScenariosWithOrderedComposition() {
        withProperties(new String[][]{
                {"eyelib.benchmark.modes", "mixed"},
                {"eyelib.benchmark.entityMix", "minecraft:slime=24,minecraft:zombie=24,minecraft:cow=48"}
        }, () -> {
            List<BenchmarkScenario> scenarios = ClientBenchmarkConfig.scenarios();

            assertEquals(2, scenarios.size());
            assertEquals("fbo-mixed-n96", scenarios.get(0).id());
            assertEquals("world-mixed-n96", scenarios.get(1).id());
            assertEquals(96, scenarios.get(0).entityCount());
            assertEquals("minecraft:slime=24,minecraft:zombie=24,minecraft:cow=48",
                    scenarios.get(0).entityComposition());
            assertEquals("mixed", scenarios.get(0).entityId());
        });
    }

    @Test
    void mixedModeRejectsDuplicateEntityTypes() {
        withProperties(new String[][]{
                {"eyelib.benchmark.modes", "mixed"},
                {"eyelib.benchmark.entityMix", "minecraft:slime=24,minecraft:slime=24"}
        }, () -> assertThrows(IllegalArgumentException.class, ClientBenchmarkConfig::scenarios));
    }

    private static void withProperties(String[][] properties, Runnable action) {
        String[] previous = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            previous[i] = System.getProperty(properties[i][0]);
            System.setProperty(properties[i][0], properties[i][1]);
        }
        try {
            action.run();
        } finally {
            for (int i = 0; i < properties.length; i++) {
                if (previous[i] == null) {
                    System.clearProperty(properties[i][0]);
                } else {
                    System.setProperty(properties[i][0], previous[i]);
                }
            }
        }
    }
}
