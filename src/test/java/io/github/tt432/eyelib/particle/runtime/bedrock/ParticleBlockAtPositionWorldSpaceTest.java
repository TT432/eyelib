package io.github.tt432.eyelib.particle.runtime.bedrock;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归：{@link BedrockParticleInstance#blockAtPosition()} 必须把发射器本地坐标
 * 换算为世界坐标再查方块（此前直接拿本地坐标查，发射器不在原点时
 * particle_expire_if_in_blocks / not_in_blocks 判定错误）。
 *
 * @author TT432
 */
class ParticleBlockAtPositionWorldSpaceTest {
    @Test
    void blockAtPositionQueriesWorldSpaceWithEmitterOffset() {
        RecordingEnvironment environment = new RecordingEnvironment();
        RecordingSpawner spawner = new RecordingSpawner();
        ParticleDefinition definition = definitionWithComponents("""
                "minecraft:emitter_rate_instant": { "num_particles": 1 },
                "minecraft:emitter_lifetime_once": { "active_time": 1 },
                "minecraft:emitter_shape_point": { "offset": [1, 2, 3] }
                """);

        BedrockParticleEmitter emitter = new BedrockParticleRuntime(definition, environment, spawner)
                .createEmitter(Optional.empty(), new Vector3f(10, 20, 30));
        emitter.onLoopStart();
        BedrockParticleInstance particle = spawner.spawned.get(0);

        particle.blockAtPosition();

        Vector3f queried = environment.lastQuery;
        // 世界坐标 = 发射器位置 (10,20,30) + 粒子本地坐标（shape offset 1,2,3）
        assertEquals(11F, queried.x, 1e-4F);
        assertEquals(22F, queried.y, 1e-4F);
        assertEquals(33F, queried.z, 1e-4F);
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
        return TestCodecUtil.unwrap(ParticleDefinitionAdapter.fromSchema(
                TestCodecUtil.unwrap(io.github.tt432.eyelib.importer.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)))
        ));
    }

    private static final class RecordingEnvironment implements ParticleRuntimeEnvironment {
        Vector3f lastQuery;

        @Override
        public int ticks() { return 0; }

        @Override
        public float partialTick() { return 0; }

        @Override
        public Optional<String> blockAtPosition(Vector3f position) {
            lastQuery = new Vector3f(position);
            return Optional.of("minecraft:stone");
        }
    }

    private static final class RecordingSpawner implements ParticleRuntimeSpawner {
        final List<BedrockParticleInstance> spawned = new ArrayList<>();

        @Override
        public void spawnParticle(BedrockParticleInstance particle) { spawned.add(particle); }
    }
}
