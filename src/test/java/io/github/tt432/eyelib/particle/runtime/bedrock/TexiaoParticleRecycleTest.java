package io.github.tt432.eyelib.particle.runtime.bedrock;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.molang.mapping.MolangMath;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * texiao 回归：{@code emitter_lifetime_once} 发射器在 active_time 后自行移除，
 * 其粒子按 {@code particle_lifetime_expression.max_lifetime} 过期。
 * 定义取自真实资源包（texiao/toulang.*.particle.json）。
 *
 * @author TT432
 */
class TexiaoParticleRecycleTest {
    private static final String[] RESOURCES = {
            "/texiao/toulang.kuosanlizi.particle.json",
            "/texiao/toulang.penjianlizi.particle.json",
            "/texiao/toulang.yanwu.particle.json",
    };

    @BeforeEach
    void setupMappingTree() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("math", MolangMath.class, true)
        ));
    }

    @AfterEach
    void tearDownMappingTree() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void oneShotEmittersSelfRemoveAndParticlesExpire() {
        for (String resource : RESOURCES) {
            ParticleDefinition definition = load(resource);
            FakeEnvironment environment = new FakeEnvironment();
            RecordingSpawner spawner = new RecordingSpawner();
            BedrockParticleEmitter emitter = new BedrockParticleRuntime(definition, environment, spawner)
                    .createEmitter(Optional.empty(), new Vector3f());

            // 推进 10 秒（200 tick），每 tick 驱动一次渲染帧（对应 ParticleRenderManager.onRenderTickStart）
            for (int tick = 0; tick < 200 && !allRecycled(emitter, spawner); tick++) {
                environment.ticks = tick;
                emitter.onRenderFrame();
                spawner.spawned.forEach(BedrockParticleInstance::onRenderFrame);
            }

            assertTrue(emitter.removed(),
                    resource + ": one-shot 发射器应在 active_time 后移除");
            assertTrue(spawner.spawned.stream().allMatch(BedrockParticleInstance::removed),
                    resource + ": 粒子应按 max_lifetime 过期");
        }
    }

    private static boolean allRecycled(BedrockParticleEmitter emitter, RecordingSpawner spawner) {
        return emitter.removed() && spawner.spawned.stream().allMatch(BedrockParticleInstance::removed);
    }

    private static ParticleDefinition load(String resource) {
        String json;
        try (var in = TexiaoParticleRecycleTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return TestCodecUtil.unwrap(ParticleDefinitionAdapter.fromSchema(
                TestCodecUtil.unwrap(io.github.tt432.eyelib.importer.particle.BrParticle.CODEC
                        .parse(JsonOps.INSTANCE, JsonParser.parseString(json)))));
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
