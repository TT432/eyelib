package io.github.tt432.eyelib.common.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class ParticleCommandRuntimeTest {
    @Test
    void suggestEffectIdsFiltersCaseInsensitivelyAndRejectsInvalidIds() {
        List<String> suggestions = ParticleCommandRuntime.suggestEffectIds(
                "eyelib:p",
                List.of("eyelib:particle", "eyelib:invalid id", "minecraft:smoke"),
                id -> !id.contains(" ")
        );

        assertIterableEquals(List.of("eyelib:particle"), suggestions);
    }

    @Test
    void buildSpawnParticleRequestUsesSupplierAndCopiesCoordinatesAsFloats() {
        ParticleCommandRuntime.SpawnParticleRequest request = ParticleCommandRuntime.buildSpawnParticleRequest(
                "eyelib:particle",
                1.25,
                2.5,
                -3.75,
                () -> "spawn-id"
        );

        assertEquals("spawn-id", request.spawnId());
        assertEquals("eyelib:particle", request.particleId());
        assertEquals(1.25F, request.x());
        assertEquals(2.5F, request.y());
        assertEquals(-3.75F, request.z());
    }

    @Test
    void spawnSuccessMessageUsesNormalizedRequestPayload() {
        ParticleCommandRuntime.SpawnParticleRequest request = new ParticleCommandRuntime.SpawnParticleRequest(
                "spawn-id",
                "eyelib:particle",
                1F,
                2F,
                3F
        );

        assertEquals("已生成粒子: eyelib:particle @ 1.0, 2.0, 3.0", ParticleCommandRuntime.spawnSuccessMessage(request));
    }
}
