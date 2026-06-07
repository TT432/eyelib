package io.github.tt432.eyelibmaterial.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 Bedrock 渲染语义转换为可缓存的 Java RenderType。
 *
 * @author TT432
 */
@NullMarked
public final class BrRenderTypeFactory {
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();

    private BrRenderTypeFactory() {
    }

    public static RenderType create(ResourceLocation texture, BrRenderState state) {
        if (!state.needsCustomRenderType()) {
            return vanilla(texture, state);
        }
        return CACHE.computeIfAbsent(new Key(texture, state), key -> custom(texture, state));
    }

    private static RenderType vanilla(ResourceLocation texture, BrRenderState state) {
        return switch (state.transparency()) {
            case NONE -> RenderType.entitySolid(texture);
            case ALPHA_TEST -> state.cull() ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
            case BLEND -> state.cull() ? RenderType.entityTranslucentCull(texture) : RenderType.entityTranslucent(texture);
            case ADDITIVE -> custom(texture, state);
        };
    }

    private static RenderType custom(ResourceLocation texture, BrRenderState state) {
        return RenderType.create(
                "eyelib_material_" + state.surfaceClass().name().toLowerCase() + "_" + texture,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                RenderType.SMALL_BUFFER_SIZE,
                false,
                state.transparency() == BrRenderState.Transparency.BLEND
                        || state.transparency() == BrRenderState.Transparency.ADDITIVE,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState(state))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(transparencyState(state))
                        .setDepthTestState(depthState(state))
                        .setCullState(new RenderStateShard.CullStateShard(state.cull()))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(
                                state.writeMask().writeColor(),
                                state.writeMask().writeDepth()))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(state.lightmap()))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(state.overlay()))
                        .createCompositeState(false)
        );
    }

    private static RenderStateShard.ShaderStateShard shaderState(BrRenderState state) {
        return new RenderStateShard.ShaderStateShard(() -> switch (state.surfaceClass()) {
            case CUTOUT -> GameRenderer.getRendertypeEntityCutoutShader();
            case EMISSIVE_CUTOUT -> GameRenderer.getRendertypeEntityTranslucentEmissiveShader();
            case TRANSLUCENT, ADDITIVE -> GameRenderer.getRendertypeEntityTranslucentShader();
            case GLINT -> GameRenderer.getRendertypeEntityGlintShader();
            default -> GameRenderer.getRendertypeEntitySolidShader();
        });
    }

    private static RenderStateShard.TransparencyStateShard transparencyState(BrRenderState state) {
        return switch (state.transparency()) {
            case NONE, ALPHA_TEST -> new RenderStateShard.TransparencyStateShard(
                    "eyelib_no_transparency",
                    () -> {},
                    () -> {}
            );
            case BLEND, ADDITIVE -> new RenderStateShard.TransparencyStateShard(
                    "eyelib_material_blend",
                    () -> {
                        BrRenderState.Blend blend = state.blend().orElse(new BrRenderState.Blend(
                                BlendFactor.SourceAlpha,
                                BlendFactor.OneMinusSrcAlpha,
                                BlendFactor.One,
                                BlendFactor.OneMinusSrcAlpha));
                        RenderSystem.enableBlend();
                        GL14.glBlendFuncSeparate(
                                blend.blendSrc().factor,
                                blend.blendDst().factor,
                                blend.alphaSrc().factor,
                                blend.alphaDst().factor);
                    },
                    RenderSystem::disableBlend
            );
        };
    }

    private static RenderStateShard.DepthTestStateShard depthState(BrRenderState state) {
        if (!state.depth().test()) {
            return new RenderStateShard.DepthTestStateShard("always", GL11.GL_ALWAYS);
        }
        return new RenderStateShard.DepthTestStateShard(
                state.depth().func().map(Enum::name).orElse("lequal"),
                state.depth().func().map(func -> func.value).orElse(GL11.GL_LEQUAL)
        );
    }

    private record Key(ResourceLocation texture, BrRenderState state) {
    }
}
