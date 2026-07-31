package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.appearance.ParticleAppearanceBillboard;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * flipbook 中 max_frame 为 Bedrock 官方可选字段（Default Value: not set，见
 * particle_appearance_billboard_flipbook_data 文档）。缺省时必须能解码并按纹理
 * 网格推导帧数，而不是抛异常拖垮渲染线程。
 * 触发场景：服务器资源包粒子 JSON 仅含 base_UV/size_UV/step_UV。
 */
class ParticleAppearanceBillboardFlipbookTest {

    @Test
    void billboardWithFlipbookMissingMaxFrameDecodes() {
        var json = JsonParser.parseString("""
                {
                  "size": [0.2, 0.2],
                  "uv": {
                    "texture_width": 1080,
                    "texture_height": 120,
                    "flipbook": {
                      "base_UV": [0, 0],
                      "size_UV": [120, 120],
                      "step_UV": [120, 0],
                      "frames_per_second": 8
                    }
                  }
                }
                """);
        ParticleAppearanceBillboard billboard =
                TestCodecUtil.unwrap(ParticleAppearanceBillboard.CODEC.parse(JsonOps.INSTANCE, json));
        assertNull(billboard.uv().flipbook().maxFrame());
    }

    @Test
    void missingMaxFrameDerivesFrameCountFromTextureGrid() {
        var json = JsonParser.parseString("""
                {
                  "size": [0.2, 0.2],
                  "uv": {
                    "texture_width": 1080,
                    "texture_height": 120,
                    "flipbook": {
                      "base_UV": [0, 0],
                      "size_UV": [120, 120],
                      "step_UV": [120, 0],
                      "frames_per_second": 8
                    }
                  }
                }
                """);
        ParticleAppearanceBillboard billboard =
                TestCodecUtil.unwrap(ParticleAppearanceBillboard.CODEC.parse(JsonOps.INSTANCE, json));
        Vector4f uv = billboard.uv().flipbook().get(new MolangScope(), 1F, 1F, 1080F, 120F);
        // 9 列 1 行 → 最后一帧索引 8；time=1、fps=8 → frame=8，恰为最后一帧
        assertEquals(8F * 120F, uv.x, 0.001F);
        assertEquals(0F, uv.y, 0.001F);
        assertEquals(120F, uv.z, 0.001F);
        assertEquals(120F, uv.w, 0.001F);
    }

    @Test
    void explicitMaxFrameStillTakesPrecedence() {
        var json = JsonParser.parseString("""
                {
                  "size": [0.2, 0.2],
                  "uv": {
                    "texture_width": 1080,
                    "texture_height": 120,
                    "flipbook": {
                      "base_UV": [0, 0],
                      "size_UV": [120, 120],
                      "step_UV": [120, 0],
                      "frames_per_second": 8,
                      "max_frame": 4
                    }
                  }
                }
                """);
        ParticleAppearanceBillboard billboard =
                TestCodecUtil.unwrap(ParticleAppearanceBillboard.CODEC.parse(JsonOps.INSTANCE, json));
        Vector4f uv = billboard.uv().flipbook().get(new MolangScope(), 1F, 1F, 1080F, 120F);
        // 显式 max_frame=4 → 最后一帧索引 3；frame=8 被 clamp 到 3
        assertEquals(3F * 120F, uv.x, 0.001F);
    }

    @Test
    void missingMaxFrameWithStretchToLifetime() {
        var json = JsonParser.parseString("""
                {
                  "size": [0.2, 0.2],
                  "uv": {
                    "texture_width": 1080,
                    "texture_height": 120,
                    "flipbook": {
                      "base_UV": [0, 0],
                      "size_UV": [120, 120],
                      "step_UV": [120, 0],
                      "stretch_to_lifetime": true
                    }
                  }
                }
                """);
        ParticleAppearanceBillboard billboard =
                TestCodecUtil.unwrap(ParticleAppearanceBillboard.CODEC.parse(JsonOps.INSTANCE, json));
        Vector4f uv = billboard.uv().flipbook().get(new MolangScope(), 2F, 1F, 1080F, 120F);
        // lifetime=2、time=1 → 50% → frame = floor(0.5 * 8) = 4
        assertEquals(4F * 120F, uv.x, 0.001F);
    }
}
