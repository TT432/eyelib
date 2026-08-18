package io.github.tt432.eyelib.particle;

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
import org.joml.Matrix4f;
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
    void updateEmitterPoseCopiesTheWorldAnchorTransform() {
        ParticleRenderManager manager = new ParticleRenderManager(ParticleClientRuntimeServices.immediate());
        BedrockParticleEmitter emitter = emitter(new FakeEnvironment(), manager);
        manager.spawnEmitter("anchor", emitter);
        Matrix4f pose = new Matrix4f().translation(4, 5, 6).rotateY((float) (Math.PI / 2));

        manager.updateEmitterPose("anchor", pose);
        pose.identity();

        assertEquals(new Vector3f(4, 5, 6), emitter.position());
        assertTrue(emitter.baseRotation().equals(
                new Matrix4f().translation(4, 5, 6).rotateY((float) (Math.PI / 2)), 0.0001F));
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

    @Test
    void updateEmitterPoseSkipsSubmitForUnknownOrRemovedEmitters() {
        // 动画层每帧对全部历史登记调 updatePose；判活缺失时渲染线程任务队列随
        // 动画循环次数线性膨胀（2026-08-18 反馈）。契约：未知/已移除 id 不提交任务。
        java.util.concurrent.atomic.AtomicInteger submitted = new java.util.concurrent.atomic.AtomicInteger();
        ParticleRenderManager manager = new ParticleRenderManager(action -> {
            submitted.incrementAndGet();
            action.run();
        });
        FakeEnvironment environment = new FakeEnvironment();

        manager.updateEmitterPose("missing", new Matrix4f());
        assertEquals(0, submitted.get(), "unknown id must not submit");

        manager.spawnEmitter("e", emitter(environment, manager));
        manager.updateEmitterPose("e", new Matrix4f());
        assertEquals(2, submitted.get(), "live emitter accepts spawn + pose submits");

        manager.removeEmitter("e");
        manager.updateEmitterPose("e", new Matrix4f());
        assertEquals(3, submitted.get(), "explicitly removed id must not submit");
    }

    @Test
    void updateEmitterPoseStopsSubmittingAfterRenderTickPurge() {
        java.util.concurrent.atomic.AtomicInteger submitted = new java.util.concurrent.atomic.AtomicInteger();
        ParticleRenderManager manager = new ParticleRenderManager(action -> {
            submitted.incrementAndGet();
            action.run();
        });
        FakeEnvironment environment = new FakeEnvironment();
        BedrockParticleEmitter emitter = emitter(environment, manager);
        manager.spawnEmitter("e", emitter);

        emitter.remove();
        manager.onRenderTickStart();

        manager.updateEmitterPose("e", new Matrix4f());
        assertEquals(1, submitted.get(), "purged (expired) emitter must not accept pose submits");
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
