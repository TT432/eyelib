package io.github.tt432.eyelib.bridge.material;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
//?} else {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import io.github.tt432.eyelib.material.gl.stencil.Face;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.neoforged.neoforge.client.stencil.StencilOperation;
import net.neoforged.neoforge.client.stencil.StencilPerFaceTest;
import net.neoforged.neoforge.client.stencil.StencilTest;
//?}
import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.render.BrRenderState;
//? if <26.1 {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
//?}
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
//? if <26.1 {
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
//?}

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 Bedrock 渲染语义转换为 PortRenderPass（内部缓存 MC RenderType）。
 *
 * @author TT432
 */
public final class BrRenderTypeFactory {
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();
    //? if <26.1 {
    //?} else {
    private static final Map<BrRenderState, RenderPipeline> PIPELINE_CACHE = new ConcurrentHashMap<>();
    //?}

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
        //? if <26.1 {
        ResourceLocation mcTex = ResourceLocationBridge.toMc(texture);
        if (!state.needsCustomRenderType()) {
            return toPortPass(state);
        }
        RenderType renderType = CACHE.computeIfAbsent(new Key(texture, state), key -> custom(mcTex, state));
        PortRenderPass pass = toPortPass(state);
        return new BridgeRenderPass(pass.transparency(), pass.disableCulling(), renderType);
        //?} else {
        Identifier mcTex = ResourceLocationBridge.toMc(texture);
        if (!state.needsCustomRenderType()) {
            return toPortPass(state);
        }
        RenderType renderType = CACHE.computeIfAbsent(new Key(texture, state), key -> custom(mcTex, state));
        PortRenderPass pass = toPortPass(state);
        return new BridgeRenderPass(pass.transparency(), pass.disableCulling(), renderType);
        //?}
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

    //? if <26.1 {
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
    //?} else {
    private static RenderType custom(Identifier texture, BrRenderState state) {
        RenderPipeline pipeline = PIPELINE_CACHE.computeIfAbsent(state, BrRenderTypeFactory::buildPipeline);
        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture);
        if (state.lightmap()) {
            builder = builder.useLightmap();
        }
        if (state.overlay()) {
            builder = builder.useOverlay();
        }
        if (state.transparency() == BrRenderState.Transparency.BLEND
                || state.transparency() == BrRenderState.Transparency.ADDITIVE) {
            builder = builder.sortOnUpload();
        }
        return RenderType.create(
                "eyelib_material_" + state.surfaceClass().name().toLowerCase() + "_" + texture,
                builder.createRenderSetup()
        );
    }

    private static RenderPipeline buildPipeline(BrRenderState state) {
        RenderPipeline.Snippet baseSnippet = isEmissive(state.surfaceClass())
                ? RenderPipelines.ENTITY_EMISSIVE_SNIPPET
                : RenderPipelines.ENTITY_SNIPPET;
        RenderPipeline.Builder builder = RenderPipeline.builder(baseSnippet)
                .withLocation(Identifier.parse("eyelib:material_" + Integer.toHexString(state.hashCode())))
                .withCull(state.cull())
                .withColorTargetState(buildColorTargetState(state))
                .withDepthStencilState(Optional.of(buildDepthStencilState(state)));
        if (isCutout(state.surfaceClass())) {
            builder = builder.withShaderDefine("ALPHA_CUTOUT", 0.1f);
        }
        if (state.stencil().isPresent()) {
            builder = builder.withStencilTest(buildStencilTest(state.stencil().get()));
        }
        return builder.build();
    }

    private static boolean isEmissive(BrRenderState.SurfaceClass sc) {
        return sc == BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE
                || sc == BrRenderState.SurfaceClass.EMISSIVE
                || sc == BrRenderState.SurfaceClass.EMISSIVE_CUTOUT;
    }

    private static boolean isCutout(BrRenderState.SurfaceClass sc) {
        return sc == BrRenderState.SurfaceClass.CUTOUT
                || sc == BrRenderState.SurfaceClass.EMISSIVE_CUTOUT;
    }

    private static ColorTargetState buildColorTargetState(BrRenderState state) {
        BrRenderState.WriteMask wm = state.writeMask();
        int colorMask = wm.writeColor() ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_NONE;
        Optional<BlendFunction> blend = switch (state.transparency()) {
            case NONE, ALPHA_TEST -> Optional.empty();
            case BLEND, ADDITIVE -> Optional.of(buildBlendFunction(state));
        };
        return new ColorTargetState(blend, colorMask);
    }

    private static BlendFunction buildBlendFunction(BrRenderState state) {
        BrRenderState.Blend blend = state.blend().orElse(new BrRenderState.Blend(
                BlendFactor.SourceAlpha,
                BlendFactor.OneMinusSrcAlpha,
                BlendFactor.One,
                BlendFactor.OneMinusSrcAlpha));
        return new BlendFunction(
                mapSourceFactor(blend.blendSrc()),
                mapDestFactor(blend.blendDst()),
                mapSourceFactor(blend.alphaSrc()),
                mapDestFactor(blend.alphaDst())
        );
    }

    private static DepthStencilState buildDepthStencilState(BrRenderState state) {
        CompareOp func = state.depth().test()
                ? state.depth().func().map(BrRenderTypeFactory::mapCompareOp).orElse(CompareOp.LESS_THAN_OR_EQUAL)
                : CompareOp.ALWAYS_PASS;
        return new DepthStencilState(func, state.writeMask().writeDepth());
    }

    private static StencilTest buildStencilTest(ResolvedBrMaterial.StencilState ss) {
        return new StencilTest(
                buildPerFace(ss.frontFace()),
                buildPerFace(ss.backFace()),
                ss.stencilReadMask(),
                ss.stencilWriteMask(),
                ss.stencilRefOverride()
        );
    }

    private static StencilPerFaceTest buildPerFace(Face face) {
        return new StencilPerFaceTest(
                mapStencilOp(face.stencilFailOp()),
                mapStencilOp(face.stencilDepthFailOp()),
                mapStencilOp(face.stencilPassOp()),
                mapStencilOpCompare(face.stencilFunc())
        );
    }

    private static SourceFactor mapSourceFactor(BlendFactor factor) {
        return switch (factor) {
            case DestColor -> SourceFactor.DST_COLOR;
            case SourceColor -> SourceFactor.SRC_COLOR;
            case Zero -> SourceFactor.ZERO;
            case One -> SourceFactor.ONE;
            case OneMinusDestColor -> SourceFactor.ONE_MINUS_DST_COLOR;
            case OneMinusSrcColor -> SourceFactor.ONE_MINUS_SRC_COLOR;
            case SourceAlpha -> SourceFactor.SRC_ALPHA;
            case DestAlpha -> SourceFactor.DST_ALPHA;
            case OneMinusSrcAlpha -> SourceFactor.ONE_MINUS_SRC_ALPHA;
        };
    }

    private static DestFactor mapDestFactor(BlendFactor factor) {
        return switch (factor) {
            case DestColor -> DestFactor.DST_COLOR;
            case SourceColor -> DestFactor.SRC_COLOR;
            case Zero -> DestFactor.ZERO;
            case One -> DestFactor.ONE;
            case OneMinusDestColor -> DestFactor.ONE_MINUS_DST_COLOR;
            case OneMinusSrcColor -> DestFactor.ONE_MINUS_SRC_COLOR;
            case SourceAlpha -> DestFactor.SRC_ALPHA;
            case DestAlpha -> DestFactor.DST_ALPHA;
            case OneMinusSrcAlpha -> DestFactor.ONE_MINUS_SRC_ALPHA;
        };
    }

    private static CompareOp mapCompareOp(DepthFunc func) {
        return switch (func) {
            case LessEqual -> CompareOp.LESS_THAN_OR_EQUAL;
            case Less -> CompareOp.LESS_THAN;
            case Equal -> CompareOp.EQUAL;
            case NotEqual -> CompareOp.NOT_EQUAL;
            case Greater -> CompareOp.GREATER_THAN;
            case GreaterEqual -> CompareOp.GREATER_THAN_OR_EQUAL;
            case Always -> CompareOp.ALWAYS_PASS;
        };
    }

    private static CompareOp mapStencilOpCompare(io.github.tt432.eyelib.material.gl.stencil.StencilFunc func) {
        return switch (func) {
            case LessEqual -> CompareOp.LESS_THAN_OR_EQUAL;
            case Less -> CompareOp.LESS_THAN;
            case Equal -> CompareOp.EQUAL;
            case NotEqual -> CompareOp.NOT_EQUAL;
            case Greater -> CompareOp.GREATER_THAN;
            case GreaterEqual -> CompareOp.GREATER_THAN_OR_EQUAL;
            case Always -> CompareOp.ALWAYS_PASS;
        };
    }

    private static StencilOperation mapStencilOp(io.github.tt432.eyelib.material.gl.stencil.StencilFailOp op) {
        return op == io.github.tt432.eyelib.material.gl.stencil.StencilFailOp.Keep
                ? StencilOperation.KEEP : StencilOperation.REPLACE;
    }

    private static StencilOperation mapStencilOp(io.github.tt432.eyelib.material.gl.stencil.StencilDepthFailOp op) {
        return op == io.github.tt432.eyelib.material.gl.stencil.StencilDepthFailOp.Keep
                ? StencilOperation.KEEP : StencilOperation.REPLACE;
    }

    private static StencilOperation mapStencilOp(io.github.tt432.eyelib.material.gl.stencil.StencilPassOp op) {
        return op == io.github.tt432.eyelib.material.gl.stencil.StencilPassOp.Keep
                ? StencilOperation.KEEP : StencilOperation.REPLACE;
    }
    //?}

    private record Key(PortResourceLocation texture, BrRenderState state) {
    }
}
