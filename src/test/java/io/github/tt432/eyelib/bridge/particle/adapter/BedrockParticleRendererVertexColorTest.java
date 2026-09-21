package io.github.tt432.eyelib.bridge.particle.adapter;
//? if <1.20.6 {

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归（9.21 反馈：粒子黑块、渐出变渐入）：1.20.1 的 VertexConsumer 颜色参数是 0..1 的 float，
 * 直接把 ARGB 拆出的 0..255 int 传进去会被 BufferBuilder 强转 byte 溢出成乱色。
 */
class BedrockParticleRendererVertexColorTest {

    @Test
    void vertexColorsAreNormalizedFloats() {
        CapturingVertexConsumer consumer = new CapturingVertexConsumer();

        BedrockParticleRenderer.vertex(consumer, new Vector3f(), 0xFFFFE7E7, 0, 0, 0, new Vector3f(0, 0, 1));

        assertEquals(1.0F, consumer.r, 1.0E-6F);
        assertEquals(231 / 255.0F, consumer.g, 1.0E-6F);
        assertEquals(231 / 255.0F, consumer.b, 1.0E-6F);
        assertEquals(1.0F, consumer.a, 1.0E-6F);
    }

    @Test
    void lowAlphaColorKeepsFadeOutGradient() {
        CapturingVertexConsumer consumer = new CapturingVertexConsumer();

        BedrockParticleRenderer.vertex(consumer, new Vector3f(), 0x26FFFFFF, 0, 0, 0, new Vector3f(0, 0, 1));

        assertEquals(0x26 / 255.0F, consumer.a, 1.0E-6F);
        assertTrue(consumer.r >= 0 && consumer.r <= 1
                && consumer.g >= 0 && consumer.g <= 1
                && consumer.b >= 0 && consumer.b <= 1,
                "颜色分量必须落在 0..1，否则 byte 强转溢出成乱色");
    }

    /** 捕获 13 参数 vertex 默认方法委托下来的 float 颜色。 */
    private static final class CapturingVertexConsumer implements VertexConsumer {
        float r, g, b, a;

        @Override
        public VertexConsumer vertex(double x, double y, double z) { return this; }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            throw new AssertionError("颜色必须走 float 路径，传 int 说明调用方没有归一化");
        }

        @Override
        public VertexConsumer color(float red, float green, float blue, float alpha) {
            this.r = red;
            this.g = green;
            this.b = blue;
            this.a = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) { return this; }

        @Override
        public VertexConsumer overlayCoords(int u, int v) { return this; }

        @Override
        public VertexConsumer uv2(int u, int v) { return this; }

        @Override
        public VertexConsumer normal(float x, float y, float z) { return this; }

        @Override
        public void endVertex() {}

        @Override
        public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultAlpha) {}

        @Override
        public void unsetDefaultColor() {}
    }
}
//?}
