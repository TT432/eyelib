//? if <1.20.6 {
package io.github.tt432.eyelib.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.adapter.TextureColorWorldPass;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** 真实 GPU 像素验证：方向/光照独立、alpha 混合以及晚绘制深度遮挡。 */
@ClientSmoke(description = "原色材质像素、透明度、世界队列深度验证", priority = 1)
public class TextureColorSmoke {
    public TextureColorSmoke() {
        ClientSmokeVisualHooks.set(mc -> {
            NativeImage pixels = new NativeImage(1, 1, false);
            pixels.setPixelRGBA(0, 0, 0xFFE08040);
            ResourceLocation texture = new ResourceLocation("eyelib", "texture_color_smoke");
            mc.getTextureManager().register(texture, new DynamicTexture(pixels));
            Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            VertexSorting sorting = RenderSystem.getVertexSorting();
            PoseStack mv = RenderSystem.getModelViewStack();
            mv.pushPose();
            try {
                mv.setIdentity(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.DISTANCE_TO_ORIGIN);
                RenderSystem.clearColor(0, 0, 0, 1);
                RenderSystem.depthMask(true);
                RenderSystem.clear(16640, false);
                TextureColorWorldPass.begin();
                RenderSink sink = RenderSink.of(mc.renderBuffers().bufferSource());
                PortResourceLocation tex = PortResourceLocation.of("eyelib", "texture_color_smoke");
                submit(sink, tex, "eyelib:texture_unlit", new QuadSpec(-.9F, -.5F, 1, 0, 0, 0));
                submit(sink, tex, "eyelib:texture_unlit", new QuadSpec(-.4F, 0F, 1, 15728880, 1, 0));
                submit(sink, tex, "eyelib:texture_unlit", new QuadSpec(.1F, .4F, .5F, 0, 0, 0));
                submit(sink, tex, "eyelib:texture_unlit_opaque", new QuadSpec(.5F, .9F, 1, 0, 0, -.5F));
                submit(sink, tex, "eyelib:texture_unlit_add", new QuadSpec(.5F, .9F, 1, 0, 0, .5F));
                sink.flush();
                TextureColorWorldPass.draw();
            } finally {
                TextureColorWorldPass.clear();
                mv.popPose(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, sorting);
                mc.getTextureManager().release(texture);
            }
        }, image -> {
            check(image, .15F, 64, 128, 224);
            check(image, .40F, 64, 128, 224);
            check(image, .625F, 32, 64, 112);
            check(image, .85F, 64, 128, 224);
        });
    }

    static void submit(RenderSink sink, PortResourceLocation tex, String material, QuadSpec spec) {
        var pass = RenderTypeResolver.resolve(PortResourceLocation.parse(material)).factory().apply(tex);
        sink.submit(pass, tex, new PoseStack(), (pose, consumer, skinning) -> {
            if (skinning != null) throw new AssertionError("原色必须走 CPU 顶点路径");
            vertex(consumer, spec.left(), -.8F, spec.z(), spec.alpha(), spec.light(), spec.normal());
            vertex(consumer, spec.right(), -.8F, spec.z(), spec.alpha(), spec.light(), spec.normal());
            vertex(consumer, spec.right(), .8F, spec.z(), spec.alpha(), spec.light(), spec.normal());
            vertex(consumer, spec.left(), .8F, spec.z(), spec.alpha(), spec.light(), spec.normal());
        });
    }

    record QuadSpec(float left, float right, float alpha, int light, float normal, float z) {
    }

    private static void vertex(VertexConsumer c, float x, float y, float z, float alpha, int light, float normal) {
        c.vertex(x, y, z, 1, 1, 1, alpha, .5F, .5F, 0, light, normal, 1 - normal, 0);
    }

    static void check(NativeImage image, float x, int r, int g, int b) {
        int pixel = image.getPixelRGBA((int) (image.getWidth() * x), image.getHeight() / 2);
        if (Math.abs((pixel & 255) - r) > 2 || Math.abs(((pixel >>> 8) & 255) - g) > 2
                || Math.abs(((pixel >>> 16) & 255) - b) > 2) {
            throw new AssertionError("原色像素不匹配 x=" + x + " actual=" + Integer.toHexString(pixel));
        }
    }
}
//?}
