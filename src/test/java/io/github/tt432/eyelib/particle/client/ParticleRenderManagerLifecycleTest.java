package io.github.tt432.eyelib.particle.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleInstance;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleRuntime;
import io.github.tt432.eyelib.particle.runtime.bedrock.ParticleRuntimeEnvironment;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleRenderManagerLifecycleTest {
    @Test
    void spawnAndRemoveOperationsUseSubmittedCollectionMutations() {
        ParticleRenderManager manager = new ParticleRenderManager(ParticleClientRuntimeServices.immediate());
        FakeEnvironment environment = new FakeEnvironment();
        BedrockParticleEmitter first = emitter(environment, manager);
        BedrockParticleEmitter duplicate = emitter(environment, manager);

        manager.spawnEmitter("test", first);
        manager.spawnEmitter("test", duplicate);

        assertEquals(1, manager.getEmitterCount(), "duplicate emitter ids must be ignored");

        manager.removeEmitter("missing");
        assertEquals(1, manager.getEmitterCount(), "removing a missing id must be a no-op");

        manager.removeEmitter("test");
        assertEquals(0, manager.getEmitterCount());
    }

    @Test
    void renderTickRemovesDeadEntriesBeforeAdvancingRemainingParticles() {
        ParticleRenderManager manager = new ParticleRenderManager(ParticleClientRuntimeServices.immediate());
        FakeEnvironment environment = new FakeEnvironment();
        BedrockParticleEmitter removedEmitter = emitter(environment, manager);
        removedEmitter.remove();
        manager.spawnEmitter("removed", removedEmitter);

        BedrockParticleEmitter particleOwner = emitter(environment, manager);
        particleOwner.onLoopStart();

        assertEquals(1, manager.getEmitterCount());
        assertEquals(1, manager.getParticleCount());

        environment.ticks = 50;
        manager.onRenderTickStart();

        assertEquals(0, manager.getEmitterCount(), "removed emitters are pruned before render-frame work");
        assertEquals(1, manager.getParticleCount(), "particles removed during frame remain until the next cleanup pass");
        manager.renderAfterEntities(particle -> assertTrue(particle.removed()));

        manager.onRenderTickStart();
        assertEquals(0, manager.getParticleCount(), "dead particles are pruned before the next frame");
    }

    @Test
    void clientTickAdvancesEmittersAndLogoutClearDropsAllState() {
        ParticleRenderManager manager = new ParticleRenderManager(ParticleClientRuntimeServices.immediate());
        FakeEnvironment environment = new FakeEnvironment();
        BedrockParticleEmitter emitter = emitter(environment, manager);

        manager.spawnEmitter("test", emitter);
        emitter.onLoopStart();

        manager.onClientTickStart();

        assertEquals(0.05F, emitter.lifetimeSeconds());
        assertEquals(1, manager.getEmitterCount());
        assertEquals(1, manager.getParticleCount());

        manager.clear();

        assertEquals(0, manager.getEmitterCount());
        assertEquals(0, manager.getParticleCount());
    }

    private static BedrockParticleEmitter emitter(FakeEnvironment environment, ParticleRenderManager manager) {
        return new BedrockParticleRuntime(definition(), environment, manager::spawnParticle)
                .createEmitter(Optional.of(new MolangScope()), new Vector3f());
    }

    private static ParticleDefinition definition() {
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
                      "minecraft:emitter_rate_instant": { "num_particles": 1 },
                      "minecraft:emitter_lifetime_once": { "active_time": 1 },
                      "minecraft:emitter_shape_point": { "offset": [0, 0, 0] },
                      "minecraft:particle_lifetime_expression": { "max_lifetime": 1 }
                    }
                  }
                }
                """;
        return TestCodecUtil.unwrap(ParticleDefinitionAdapter.fromSchema(
                TestCodecUtil.unwrap(io.github.tt432.eyelib.importer.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)))
        ));
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
}
