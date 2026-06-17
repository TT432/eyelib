package io.github.tt432.eyelib.particle.runtime.bedrock;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleInitializationTest {
    @Test
    void onLoopStartSetsParticleMolangVariablesSpeedAndLifetime() {
        FakeEnvironment environment = new FakeEnvironment();
        RecordingSpawner spawner = new RecordingSpawner();
        ParticleDefinition definition = definitionWithComponents("""
                "minecraft:emitter_rate_instant": { "num_particles": 1 },
                "minecraft:emitter_lifetime_once": { "active_time": 1 },
                "minecraft:emitter_shape_point": { "offset": [0, 0, 0] },
                "minecraft:particle_initial_speed": 3,
                "minecraft:particle_lifetime_expression": { "max_lifetime": 2 }
                """);

        BedrockParticleEmitter emitter = new BedrockParticleRuntime(definition, environment, spawner)
                .createEmitter(Optional.empty(), new Vector3f());
        emitter.onLoopStart();
        BedrockParticleInstance particle = spawner.spawned.get(0);

        assertTrue(particle.molangScope().contains("variable.particle_age"));
        assertTrue(particle.molangScope().contains("variable.particle_lifetime"));
        assertTrue(particle.molangScope().contains("variable.particle_random_4"));
        assertEquals(3F, particle.speed());
        assertEquals(2F, particle.lifetime());
    }

    private static ParticleDefinition definitionWithComponents(String componentsJson) {
        String json = """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "eyelib:test_particle",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "textures/particle/test"
                      }
                    },
                    "components": {
                      %s
                    }
                  }
                }
                """.formatted(componentsJson);
        return ParticleDefinitionAdapter.fromSchema(
                io.github.tt432.eyelib.importer.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                        .getOrThrow(false, AssertionError::new)
        ).getOrThrow(false, AssertionError::new);
    }

    private static final class FakeEnvironment implements ParticleRuntimeEnvironment {
        int ticks;
        @Override
        public int ticks() { return ticks; }
        @Override
        public float partialTick() { return 0; }
    }

    private static final class RecordingSpawner implements ParticleRuntimeSpawner {
        final List<BedrockParticleInstance> spawned = new ArrayList<>();
        @Override
        public void spawnParticle(BedrockParticleInstance particle) { spawned.add(particle); }
    }
}
