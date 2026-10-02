//? if <1.20.6 {
package io.github.tt432.eyelib.bridge.material.adapter;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.material.BridgeRenderPass;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 1.20.1 贴图原色材质；材质标记同时供世界延迟提交使用。 */
public final class TextureColorMaterial {
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();
    private static final Map<RenderType, PortRenderPass.Transparency> TYPES = new ConcurrentHashMap<>();

    private TextureColorMaterial() {}

    public static PortRenderPass create(PortResourceLocation texture, PortRenderPass.Transparency transparency) {
        RenderType type = CACHE.computeIfAbsent(new Key(texture, transparency), key -> {
            boolean blend = transparency == PortRenderPass.Transparency.TRANSLUCENT
                    || transparency == PortRenderPass.Transparency.ADDITIVE;
            RenderType result = RenderType.create("eyelib_texture_color_" + transparency + "_" + texture,
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, blend,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> ParticleUnlitShaders.textureColorShader(transparency)))
                            .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocationBridge.toMc(texture), false, false))
                            .setTransparencyState(new RenderStateShard.TransparencyStateShard("texture_color_blend", () -> {
                                if (blend) {
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,
                                            transparency == PortRenderPass.Transparency.ADDITIVE ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA,
                                            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                } else {
                                    RenderSystem.disableBlend();
                                }
                            }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); }))
                            .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal", GL11.GL_LEQUAL))
                            .setCullState(new RenderStateShard.CullStateShard(false))
                            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, !blend))
                            .createCompositeState(false));
            TYPES.put(result, transparency);
            return result;
        });
        return new BridgeRenderPass(transparency, true, type);
    }

    public static boolean contains(RenderType type) {
        return TYPES.containsKey(type);
    }

    public static boolean isBlended(RenderType type) {
        PortRenderPass.Transparency value = TYPES.get(type);
        return value == PortRenderPass.Transparency.TRANSLUCENT || value == PortRenderPass.Transparency.ADDITIVE;
    }

    private record Key(PortResourceLocation texture, PortRenderPass.Transparency transparency) {}
}
//?}
