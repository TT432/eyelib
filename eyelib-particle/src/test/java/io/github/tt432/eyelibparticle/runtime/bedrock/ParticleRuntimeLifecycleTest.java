package io.github.tt432.eyelibparticle.runtime.bedrock;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRuntimeLifecycleTest {
    @Test
    void emitterRegistersMolangStateAndSpawnsModuleParticles() {
        FakeEnvironment environment = new FakeEnvironment();
        RecordingSpawner spawner = new RecordingSpawner();
        ParticleDefinition definition = definitionWithComponents("""
                "minecraft:emitter_rate_instant": { "num_particles": 2 },
                "minecraft:emitter_lifetime_once": { "active_time": 1 },
                "minecraft:emitter_shape_point": { "offset": [1, 2, 3] }
                """);

        BedrockParticleRuntime runtime = new BedrockParticleRuntime(definition, environment, spawner);
        MolangScope parentScope = new MolangScope();
        parentScope.set("parent_marker", 42);
        BedrockParticleEmitter emitter = runtime.createEmitter(Optional.of(parentScope), new Vector3f(4, 5, 6));

        assertTrue(emitter.molangScope().contains("parent_marker"));
        assertTrue(emitter.molangScope().contains("variable.emitter_age"));
        assertTrue(emitter.molangScope().contains("variable.emitter_lifetime"));
        assertTrue(emitter.molangScope().contains("variable.emitter_random_4"));
        assertFalse(emitter.removed());

        emitter.onLoopStart();

        assertEquals(2, emitter.emitCount());
        assertEquals(2, spawner.spawned.size());
        assertEquals(new Vector3f(1, 2, 3), spawner.spawned.get(0).position());

        spawner.spawned.get(0).remove();
        assertEquals(1, emitter.emitCount());
        spawner.spawned.get(0).remove();
        assertEquals(1, emitter.emitCount(), "particle remove callback must be idempotent");

        emitter.remove();
        assertTrue(emitter.removed());
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
                io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                        .getOrThrow(false, AssertionError::new)
        ).getOrThrow(false, AssertionError::new);
    }

    private static final class FakeEnvironment implements ParticleRuntimeEnvironment {
        int ticks;

        @Override
        public int ticks() {
            return ticks;
        }

        @Override
        public float partialTick() {
            return 0;
        }
    }

    private static final class RecordingSpawner implements ParticleRuntimeSpawner {
        final List<BedrockParticleInstance> spawned = new ArrayList<>();

        @Override
        public void spawnParticle(BedrockParticleInstance particle) {
            spawned.add(particle);
        }
    }
}
