package io.github.tt432.eyelibbridge.material;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import io.github.tt432.eyelibmaterial.port.PortRenderPass;
import io.github.tt432.eyelibutil.PortResourceLocation;
import io.github.tt432.eyelibmaterial.render.BrRenderState;
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
 * 将 Bedrock 渲染语义转换为 PortRenderPass（内部缓存 MC RenderType）。
 *
 * @author TT432
 */
@NullMarked
public final class BrRenderTypeFactory {
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();

    private BrRenderTypeFactory() {
    }

    /**
     * 根据 Bedrock 渲染状态创建 PortRenderPass。
     * 内部将 PortResourceLocation 转换为 MC ResourceLocation 以供缓存/使用。
     *
     * @param texture 纹理标识
     * @param state   Bedrock 渲染状态
     * @return 语义化 PortRenderPass
     */
    public static PortRenderPass create(PortResourceLocation texture, BrRenderState state) {
        ResourceLocation mcTex = ResourceLocationBridge.toMc(texture);
        if (!state.needsCustomRenderType()) {
            return toPortPass(state);
        }
        RenderType renderType = CACHE.computeIfAbsent(new Key(texture, state), key -> custom(mcTex, state));
        PortRenderPass pass = toPortPass(state);
        return new BridgeRenderPass(pass.transparency(), pass.disableCulling(), renderType);
    }

    private static PortRenderPass toPortPass(BrRenderState state) {
        return PortRenderPass.of(
                switch (state.transparency()) {
                    case NONE -> PortRenderPass.Transparency.SOLID;
                    case ALPHA_TEST -> PortRenderPass.Transparency.ALPHA_TEST;
                    case BLEND -> state.surfaceClass() == BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE
                            ? PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE
                            : PortRenderPass.Transparency.TRANSLUCENT;
                    case ADDITIVE -> PortRenderPass.Transparency.ADDITIVE;
                },
                !state.cull()
        );
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
            case EMISSIVE_CUTOUT, TRANSLUCENT_EMISSIVE -> GameRenderer.getRendertypeEntityTranslucentEmissiveShader();
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

    private record Key(PortResourceLocation texture, BrRenderState state) {
    }
}
