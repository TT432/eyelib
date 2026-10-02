//? if <1.20.6 {
package io.github.tt432.eyelib.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.bridge.client.compat.oculus.OculusCompat;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.adapter.TextureColorWorldPass;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;

/** 经真实世界事件入队，由 GameRenderer mixin 提交，验证光影合成后的调用顺序。 */
@ClientSmoke(description = "世界渲染钩子与光影合成顺序像素验证", priority = 2)
public class TextureColorWorldSmoke {
    public TextureColorWorldSmoke() { // NOPMD — 世界渲染 smoke 必须在构造时安装生命周期回调
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = new ResourceLocation("eyelib", "texture_color_world_smoke");
        NativeImage pixels = new NativeImage(1, 1, false);
        pixels.setPixelRGBA(0, 0, 0xFFE08040);
        mc.getTextureManager().register(texture, new DynamicTexture(pixels));
        int[] frames = {0};
        java.util.function.Consumer<RenderLevelStageEvent> listener = event -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || OculusCompat.renderingShadowPass()) return;
            if (!TextureColorWorldPass.collecting()) throw new AssertionError("世界帧未开启原色队列");
            frames[0]++;
            Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            VertexSorting sorting = RenderSystem.getVertexSorting();
            PoseStack mv = RenderSystem.getModelViewStack();
            mv.pushPose();
            try {
                mv.setIdentity(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.DISTANCE_TO_ORIGIN);
                RenderSink sink = RenderSink.of(mc.renderBuffers().bufferSource());
                PortResourceLocation tex = PortResourceLocation.of("eyelib", "texture_color_world_smoke");
                var background = RenderTypeResolver.resolve(PortResourceLocation.parse("eyelib:texture_unlit_opaque")).factory().apply(tex);
                sink.submit(background, tex, new PoseStack(), (pose, c, skin) -> {
                    c.vertex(-.95F, -.85F, -.98F, 0, 0, 0, 1, .5F, .5F, 0, 0, 0, 1, 0);
                    c.vertex(.95F, -.85F, -.98F, 0, 0, 0, 1, .5F, .5F, 0, 0, 0, 1, 0);
                    c.vertex(.95F, .85F, -.98F, 0, 0, 0, 1, .5F, .5F, 0, 0, 0, 1, 0);
                    c.vertex(-.95F, .85F, -.98F, 0, 0, 0, 1, .5F, .5F, 0, 0, 0, 1, 0);
                });
                TextureColorSmoke.submit(sink, tex, "eyelib:texture_unlit", new TextureColorSmoke.QuadSpec(-.9F, -.5F, 1, 0, 0, -.99F));
                TextureColorSmoke.submit(sink, tex, "eyelib:texture_unlit", new TextureColorSmoke.QuadSpec(-.4F, 0F, 1, 15728880, 1, -.99F));
                TextureColorSmoke.submit(sink, tex, "eyelib:texture_unlit", new TextureColorSmoke.QuadSpec(.1F, .4F, .5F, 0, 0, -.99F));
                TextureColorSmoke.submit(sink, tex, "eyelib:texture_unlit_opaque", new TextureColorSmoke.QuadSpec(.5F, .9F, 1, 0, 0, -.999F));
                TextureColorSmoke.submit(sink, tex, "eyelib:texture_unlit_add", new TextureColorSmoke.QuadSpec(.5F, .9F, 1, 0, 0, -.99F));
            } finally {
                mv.popPose(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, sorting);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        ClientSmokeVisualHooks.set(m -> {}, image -> {
            try {
                if (frames[0] == 0) throw new AssertionError("截图前没有执行世界入队");
                TextureColorSmoke.check(image, .15F, 64, 128, 224);
                TextureColorSmoke.check(image, .40F, 64, 128, 224);
                TextureColorSmoke.check(image, .625F, 32, 64, 112);
                TextureColorSmoke.check(image, .85F, 64, 128, 224);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                mc.getTextureManager().release(texture);
            }
        });
    }
}
//?}
