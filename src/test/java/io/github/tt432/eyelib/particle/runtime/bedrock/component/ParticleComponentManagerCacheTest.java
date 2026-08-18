package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.importer.particle.BrParticle;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link ParticleComponentManager} 按 definition 缓存组件解码结果：
 * 渲染线程每帧每粒子都会取组件列表，重复 codec decode（含 molang 编译）曾是主要热点。
 */
class ParticleComponentManagerCacheTest {
    @Test
    void particleComponentsAreDecodedOncePerDefinition() {
        ParticleDefinition definition = definitionWithComponents("""
                "minecraft:particle_initial_speed": 3
                """);

        List<ParticleParticleComponent> first = ParticleComponentManager.particleComponents(definition);
        List<ParticleParticleComponent> second = ParticleComponentManager.particleComponents(definition);

        assertSame(first, second);
        assertEquals(1, first.size());
    }

    @Test
    void equalDefinitionSharesCachedComponents() {
        String components = """
                "minecraft:particle_initial_speed": 3
                """;
        ParticleDefinition a = definitionWithComponents(components);
        ParticleDefinition b = definitionWithComponents(components);

        assertSame(
                ParticleComponentManager.particleComponents(a),
                ParticleComponentManager.particleComponents(b)
        );
    }

    @Test
    void differentComponentsProduceDifferentLists() {
        ParticleDefinition speedOnly = definitionWithComponents("""
                "minecraft:particle_initial_speed": 3
                """);
        ParticleDefinition speedAndSpin = definitionWithComponents("""
                "minecraft:particle_initial_speed": 3,
                "minecraft:particle_initial_spin": { "rotation": 45, "rotation_rate": 90 }
                """);

        List<ParticleParticleComponent> a = ParticleComponentManager.particleComponents(speedOnly);
        List<ParticleParticleComponent> b = ParticleComponentManager.particleComponents(speedAndSpin);

        assertNotSame(a, b);
        assertEquals(1, a.size());
        assertEquals(2, b.size());
    }

    @Test
    void emitterComponentsAreDecodedOncePerDefinition() {
        ParticleDefinition definition = definitionWithComponents("""
                "minecraft:emitter_rate_instant": { "num_particles": 1 }
                """);

        List<EmitterParticleComponent> first = ParticleComponentManager.emitterComponents(definition);
        List<EmitterParticleComponent> second = ParticleComponentManager.emitterComponents(definition);

        assertSame(first, second);
        assertEquals(1, first.size());
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
                TestCodecUtil.unwrap(BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)))
        ));
    }
}
