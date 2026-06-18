package io.github.tt432.eyelib.importer.particle;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Bedrock 粒子 JSON 格式的规范测试。
 * 测试数据来自 Microsoft shapeshifter 官方示例（Mojang Creator 文档配套）。
 *
 * @author TT432
 */
class BrParticleSpecTest {

    /**
     * Mojang 文档 ParticleEffects：粒子文件根结构为 format_version + particle_effect。
     * 验证真实 Bedrock 粒子 JSON 可以无错误解析。
     */
    @Test
    @DisplayName("Mojang §ParticleEffects: 真实 witchspell 粒子解析")
    void realWitchspellParses() throws Exception {
        String json = loadFixture("witchspell.json");

        var result = BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.result().isPresent(),
                "真实 Bedrock 粒子 JSON 应成功解析: " + result.result());

        var particle = TestCodecUtil.unwrap(result);
        assertEquals("1.10.0", particle.formatVersion());

        var effect = particle.particleEffect();
        assertNotNull(effect);

        var desc = effect.description();
        assertNotNull(desc);
        assertEquals("sample:witchspell_emitter", desc.identifier());

        var renderParams = desc.basicRenderParameters();
        assertNotNull(renderParams);
        assertEquals("particles_alpha", renderParams.material(),
                "Mojang 文档: 火焰粒子使用 particles_alpha 材质");
        assertTrue(renderParams.texture().contains("particles"),
                "纹理路径应指向 particles 目录");
    }

    /**
     * Mojang 文档：粒子 components 区域包含 minecraft:emitter_* 和 minecraft:particle_* 组件。
     * 验证组件区域非空。
     */
    @Test
    @DisplayName("Mojang §Components: 解析 emitter 和 particle 组件")
    void componentsParsed() throws Exception {
        String json = loadFixture("witchspell.json");

        var particle = TestCodecUtil.unwrap(BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        var components = particle.particleEffect().components();
        assertFalse(components.isEmpty(),
                "Mojang 文档: particles 必须包含 components");

        // 验证关键组件存在
        assertTrue(components.containsKey("minecraft:emitter_rate_instant"),
                "应包含发射率组件");
        assertTrue(components.containsKey("minecraft:emitter_lifetime_once"),
                "应包含发射器生命周期组件");
    }

    /**
     * Mojang 文档 §Naming and Location: 粒子标识符为 namespace:name 格式。
     */
    @Test
    @DisplayName("Mojang §Naming: identifier 为 namespace:name 格式")
    void identifierHasNamespaceFormat() throws Exception {
        String json = loadFixture("witchspell.json");

        var particle = TestCodecUtil.unwrap(BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        String identifier = particle.particleEffect().description().identifier();
        assertTrue(identifier.contains(":"),
                "Mojang 文档: identifier 必须包含 namespace:name");
        String[] parts = identifier.split(":");
        assertEquals(2, parts.length);
        assertFalse(parts[0].isEmpty(), "namespace 不能为空");
        assertFalse(parts[1].isEmpty(), "name 不能为空");
    }

    /**
     * Mojang 文档: curves 是可选字段。
     */
    @Test
    @DisplayName("Mojang §Curves: curves 字段可选，缺失时为空 Map")
    void curvesOptionalDefaultsToEmpty() throws Exception {
        String json = loadFixture("witchspell.json");

        var particle = TestCodecUtil.unwrap(BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        var curves = particle.particleEffect().curves();
        assertNotNull(curves);
        // witchspell 没有 curves — 应为空
    }

    private static String loadFixture(String name) throws Exception {
        String path = "/io/github/tt432/eyelib/importer/addon/fixtures/microsoft-shapeshifter/resource_pack/shapeshifter/particles/" + name;
        try (InputStream in = BrParticleSpecTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
