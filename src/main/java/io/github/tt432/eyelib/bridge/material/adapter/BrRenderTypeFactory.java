package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.bridge.material.BridgeRenderPass;

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
import net.minecraft.client.renderer.ShaderInstance;
//?}
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
import io.github.tt432.eyelib.bridge.client.compat.oculus.OculusCompat;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
import io.github.tt432.eyelib.bridge.client.compat.iris.IrisPipelineCompat;
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

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 Bedrock 渲染语义转换为 PortRenderPass（内部缓存 MC RenderType）。
 *
 * @author TT432
 */
public final class BrRenderTypeFactory {
    private BrRenderTypeFactory() {}

    /**
     * JE 实体通道的 alpha 阈值（vanilla <26.1 各实体着色器硬编码 0.1；26.1 各实体 pipeline
     * 定义 {@code ALPHA_CUTOUT=0.1}）。
     */
    public static final float ENTITY_ALPHA_CUTOUT = 0.1F;

    /**
     * 同一个 {@link BrRenderState} 在两种通道里的 alpha test / 深度写语义不同，故构造时必须显式区分：
     * <ul>
     *   <li>{@link #ENTITY}：Bedrock 实体几何。vanilla 实体通道对「所有非不透明 pass」都 alpha test
     *       （<26.1 写死在 cutout/translucent/translucent_emissive 着色器里；26.1 由
     *       {@code ALPHA_CUTOUT} define 显式开启），且半透明实体写深度（ENTITY_TRANSLUCENT
     *       DEFAULT = 写；只有 ENTITY_TRANSLUCENT_EMISSIVE 明确不写）。</li>
     *   <li>{@link #PARTICLE}：粒子几何。26.1 vanilla 粒子 pipeline 没有 {@code ALPHA_CUTOUT}
     *       （软 alpha 粒子不能剔除），只有材质自带 {@code ALPHA_TEST} 的参与剔除。</li>
     * </ul>
     */
    public enum PassKind { ENTITY, PARTICLE }

    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();
    //? if <26.1 {
    //?} else {
    private static final Map<PipelineKey, RenderPipeline> PIPELINE_CACHE = new ConcurrentHashMap<>();
    //?}


    /**
     * 根据 Bedrock 渲染状态创建 PortRenderPass。
     * 内部将 PortResourceLocation 转换为 MC ResourceLocation 以供缓存/使用。
     *
     * @param texture 纹理标识
     * @param state   Bedrock 渲染状态
     * @param kind    通道语义（实体几何 / 粒子），决定 alpha test 与深度写口径
     * @return 语义化 PortRenderPass
     */
    public static PortRenderPass create(PortResourceLocation texture, BrRenderState state, PassKind kind) {
        BrRenderState effective = RenderTypeCapabilities.channelState(state);
        //? if <26.1 {
        ResourceLocation mcTex = ResourceLocationBridge.toMc(texture);
        if (!RenderTypeCapabilities.needsCustomRenderType(effective)) {
            return toPortPass(effective);
        }
        RenderType renderType = CACHE.computeIfAbsent(new Key(texture, effective, kind), key -> custom(mcTex, effective, kind));
        PortRenderPass pass = toPortPass(effective);
        return new BridgeRenderPass(pass.transparency(), pass.disableCulling(), renderType);
        //?} else {
        Identifier mcTex = ResourceLocationBridge.toMc(texture);
        if (!RenderTypeCapabilities.needsCustomRenderType(effective)) {
            return toPortPass(effective);
        }
        RenderType renderType = CACHE.computeIfAbsent(new Key(texture, effective, kind), key -> custom(mcTex, effective, kind));
        PortRenderPass pass = toPortPass(effective);
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

    /**
     * 该 pass 是否必须 alpha test（discard）以及阈值。
     *
     * <p>实体通道的判据不是「材质写了 ALPHA_TEST」而是「这个 pass 会不会在不该写深度的地方写深度」：
     * alpha≈0 的 texel 一旦照常光栅化，写深度的 pass 就会遮住之后绘制的半透明地形（水），
     * 不写深度的 pass 则会因绘制顺序翻转产生「z 冲突」式闪烁。两代 vanilla 的答案一致：
     * <b>只有不透明 pass 不 alpha test</b>。
     * <ul>
     *   <li><26.1：cutout / translucent / translucent_emissive 着色器都硬编码 {@code color.a < 0.1 → discard}；
     *       只有 {@code rendertype_entity_solid.fsh} 不 discard。</li>
     *   <li>26.1：统一 {@code entity.fsh} 只在 {@code #ifdef ALPHA_CUTOUT} 时 discard；vanilla 的
     *       ENTITY_CUTOUT(_CULL) / ENTITY_TRANSLUCENT(_CULL) / ENTITY_TRANSLUCENT_EMISSIVE /
     *       ARMOR_TRANSLUCENT / BREEZE_WIND / ENERGY_SWIRL 都定义 {@code ALPHA_CUTOUT=0.1}，
     *       只有 ENTITY_SOLID / GLINT 不定义。</li>
     * </ul>
     *
     * <p>粒子通道不套用该规则：26.1 vanilla 的 TRANSLUCENT_PARTICLE 没有 {@code ALPHA_CUTOUT}
     * （软 alpha 粒子必须保留边缘），仅材质显式 {@code ALPHA_TEST} 的粒子参与剔除。
     */
    static Optional<Float> alphaTest(BrRenderState state, PassKind kind) {
        if (kind == PassKind.PARTICLE) {
            return isCutout(state.surfaceClass()) ? Optional.of(ENTITY_ALPHA_CUTOUT) : Optional.empty();
        }
        return isOpaque(state.surfaceClass())
                ? Optional.empty()
                : Optional.of(ENTITY_ALPHA_CUTOUT);
    }

    /**
     * pass 的实际深度写开关。
     *
     * <p>实体通道的深度写不是自由参数：写深度 = 参与遮挡后面的几何（含半透明地形）。
     * vanilla 26.1 的 ENTITY_TRANSLUCENT 写深度（DEFAULT），只有 ENTITY_TRANSLUCENT_EMISSIVE
     * 明确 {@code DepthStencilState(LESS_THAN_OR_EQUAL, false)}；1.20.1 的
     * {@code entityTranslucentEmissive} 同样是 COLOR_WRITE。发光叠加层若写深度，会把后面的
     * 发光层/水整片剔掉，故实体通道里 {@code TRANSLUCENT_EMISSIVE} 一律不写深度。
     * 其余 surfaceClass 仍尊重 Bedrock 的 {@code DisableDepthWrite}（默认写）。
     */
    static boolean effectiveWriteDepth(BrRenderState state, PassKind kind) {
        if (kind == PassKind.ENTITY && state.surfaceClass() == BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE) {
            return false;
        }
        return state.writeMask().writeDepth();
    }

    private static boolean isOpaque(BrRenderState.SurfaceClass sc) {
        return sc == BrRenderState.SurfaceClass.OPAQUE
                || sc == BrRenderState.SurfaceClass.EMISSIVE
                || sc == BrRenderState.SurfaceClass.GLINT;
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

    /**
     * 是否走「无方向光粒子上色器」（eyelib:particle_unlit）：仅 AMBIENT / NONE。
     *
     * <p>{@link BrRenderState.LightingModel#LIGHTMAP_ONLY} 不算——它保留 vanilla entity shader，只在 26.1
     * 通过 {@code NO_CARDINAL_LIGHTING} 编译分支去掉法线方向项（ADR-0040）。
     */
    private static boolean isParticleUnlit(BrRenderState state) {
        return state.lighting() == BrRenderState.LightingModel.AMBIENT
                || state.lighting() == BrRenderState.LightingModel.NONE;
    }

    //? if <26.1 {
    private static RenderType custom(ResourceLocation texture, BrRenderState state, PassKind kind) {
        RenderType renderType = RenderType.create(
                "eyelib_material_" + state.surfaceClass().name().toLowerCase(Locale.ROOT) + "_" + texture,
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
                                effectiveWriteDepth(state, kind)))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(state.lightmap()))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(state.overlay()))
                        .createCompositeState(false)
        );
        // C1 GPU 蒙皮（P2，ADR-0032）：创建期登记 routing RenderType 的蒙皮 shader 变体；
        // 变体选择必须与下方 shaderState 的 vanilla shader 选择保持一致（蒙皮 shader 是其逐行复刻）
        LegacySkinningManager.registerVariant(renderType, legacySkinningVariant(state));
        return renderType;
    }
    /** 与 {@link #shaderState} 的 vanilla shader 选择一一对应的蒙皮变体（GLINT 等不支持的回退 CPU）。 */
    private static int legacySkinningVariant(BrRenderState state) {
        // 无方向光 state 使用 eyelib:particle_unlit（非 vanilla 逐行复刻对象），无对应蒙皮变体；
        // 粒子本就不走蒙皮，登记 UNSUPPORTED 防御误路由（ADR-0033）。
        // LIGHTMAP_ONLY 仍走 vanilla entity shader（26.1 起该 shader 支持 NO_CARDINAL_LIGHTING 编译分支），
        // 蒙皮变体照常可用（ADR-0040）。
        if (isParticleUnlit(state)) {
            return LegacySkinningManager.VARIANT_UNSUPPORTED;
        }
        return switch (state.surfaceClass()) {
            case CUTOUT -> LegacySkinningManager.VARIANT_CUTOUT;
            case EMISSIVE_CUTOUT, TRANSLUCENT_EMISSIVE ->
                    LegacySkinningManager.VARIANT_EMISSIVE;
            case TRANSLUCENT, ADDITIVE -> state.emissive()
                    ? LegacySkinningManager.VARIANT_EMISSIVE
                    : LegacySkinningManager.VARIANT_TRANSLUCENT;
            case GLINT -> LegacySkinningManager.VARIANT_UNSUPPORTED;
            default -> LegacySkinningManager.VARIANT_SOLID;
        };
    }

    private static RenderStateShard.ShaderStateShard shaderState(BrRenderState state) {
        return new RenderStateShard.ShaderStateShard(() -> {
            if (isParticleUnlit(state) && !OculusCompat.shaderPackActive()) {
                // 无方向光（粒子）：eyelib:particle_unlit；注册失败/重载窗口退化 vanilla（方向光近似），不崩
                // Oculus/Iris 光影激活时不走自定义 shader：非 Iris 自有 ShaderInstance.apply() 会被
                // DepthColorStorage 锁深度/颜色写入（零像素，OculusCompat 实证）；光影下光照模型本就由
                // pack 接管，vanilla 批次经 ExtendedShader/FallbackShader 豁免锁定，语义正确
                ShaderInstance unlit = ParticleUnlitShaders.unlitShader();
                if (unlit != null) {
                    return unlit;
                }
            }
            return switch (state.surfaceClass()) {
                case CUTOUT -> GameRenderer.getRendertypeEntityCutoutShader();
                case EMISSIVE_CUTOUT, TRANSLUCENT_EMISSIVE -> GameRenderer.getRendertypeEntityTranslucentEmissiveShader();
                // emissive 材质必须走无光照贴图的发光着色器（BE 发光=不受场景光照）；
                // entityTranslucent 会乘光照贴图，黑屋里发光层直接变暗（A&S 蜘蛛红眼教训）
                case TRANSLUCENT, ADDITIVE -> state.emissive()
                        ? GameRenderer.getRendertypeEntityTranslucentEmissiveShader()
                        : GameRenderer.getRendertypeEntityTranslucentShader();
                case GLINT -> GameRenderer.getRendertypeEntityGlintShader();
                default -> GameRenderer.getRendertypeEntitySolidShader();
            };
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
                                mapBlendFactorToGl(blend.blendSrc()),
                                mapBlendFactorToGl(blend.blendDst()),
                                mapBlendFactorToGl(blend.alphaSrc()),
                                mapBlendFactorToGl(blend.alphaDst()));
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
                state.depth().func().map(BrRenderTypeFactory::mapDepthFuncToGl).orElse(GL11.GL_LEQUAL)
        );
    }

    /** Bedrock 混合因子 → GL 常量（domain 枚举保持纯数据，ADR-0016）。 */
    private static int mapBlendFactorToGl(BlendFactor factor) {
        return switch (factor) {
            case DestColor -> GL11.GL_DST_COLOR;
            case SourceColor -> GL11.GL_SRC_COLOR;
            case Zero -> GL11.GL_ZERO;
            case One -> GL11.GL_ONE;
            case OneMinusDestColor -> GL11.GL_ONE_MINUS_DST_COLOR;
            case OneMinusSrcColor -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha -> GL11.GL_SRC_ALPHA;
            case DestAlpha -> GL11.GL_DST_ALPHA;
            case OneMinusSrcAlpha -> GL11.GL_ONE_MINUS_SRC_ALPHA;
        };
    }

    /** 深度比较函数 → GL 常量。 */
    private static int mapDepthFuncToGl(DepthFunc func) {
        return switch (func) {
            case Always -> GL11.GL_ALWAYS;
            case Equal -> GL11.GL_EQUAL;
            case NotEqual -> GL11.GL_NOTEQUAL;
            case Less -> GL11.GL_LESS;
            case Greater -> GL11.GL_GREATER;
            case GreaterEqual -> GL11.GL_GEQUAL;
            case LessEqual -> GL11.GL_LEQUAL;
        };
    }
    //?} else {
    private static RenderType custom(Identifier texture, BrRenderState state, PassKind kind) {
        RenderPipeline pipeline = PIPELINE_CACHE.computeIfAbsent(new PipelineKey(state, kind),
                key -> buildPipeline(key.state(), key.kind()));
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
        if (kind == PassKind.ENTITY) {
            // vanilla 的 entity RenderType 全部 AFFECTS_OUTLINE（发光描边依赖它）；粒子不参与实体描边
            builder = builder.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        }
        return RenderType.create(
                "eyelib_material_" + state.surfaceClass().name().toLowerCase(Locale.ROOT) + "_" + texture,
                builder.createRenderSetup()
        );
    }

    /**
     * 26.1 实体渲染主路径：按 {@link PortRenderPass} 语义构造 LIGHTMAP_ONLY 的
     * {@link BrRenderState} 并建自定义 RenderType（ADR-0040）。
     *
     * <p>不能继续直接用 vanilla {@code RenderTypes.entity*}: 26.1 的 {@code ENTITY_CUTOUT} /
     * {@code ENTITY_TRANSLUCENT} 带 {@code PER_FACE_LIGHTING}（{@code entity.vsh} 里优先级高于
     * NO_CARDINAL_LIGHTING），按世界法线给每个面 0.4~1.0 的亮度。Bedrock 纹理已烘焙面明暗，
     * 叠加后得到 0.4 暗面；骨骼动画改变法线又让整面亮度在 0.4↔1.0 间跳变（实机表现为"阴影闪烁"）。
     * 自定义 pipeline 基于 {@code ENTITY_SNIPPET}（不含 PER_FACE_LIGHTING）并加
     * {@code NO_CARDINAL_LIGHTING}，观感与 1.20.1/1.21.1 一致。
     */
    private static RenderPipeline buildPipeline(BrRenderState state, PassKind kind) {
        RenderPipeline.Snippet baseSnippet = isEmissive(state.surfaceClass())
                ? RenderPipelines.ENTITY_EMISSIVE_SNIPPET
                : RenderPipelines.ENTITY_SNIPPET;
        RenderPipeline.Builder builder = RenderPipeline.builder(baseSnippet)
                // 同一 state 在不同 PassKind 下 define（ALPHA_CUTOUT）与深度写不同 → location 必须区分，
                // 否则 Iris 按 pipeline 身份登记光影程序时会互相覆盖
                .withLocation(Identifier.parse("eyelib:material_" + Integer.toHexString(31 * state.hashCode() + kind.ordinal())))
                .withCull(state.cull())
                // entity shader 在 !NO_OVERLAY 时声明 Sampler1(overlay 纹理)；BrRenderStateFactory 强制 overlay=true，
                // 故 pipeline 必须声明 Sampler1，否则 shader 读未绑定 sampler → 全黑。vanilla 所有实体 pipeline 均如此。
                .withSampler("Sampler1")
                .withColorTargetState(buildColorTargetState(state))
                .withDepthStencilState(Optional.of(buildDepthStencilState(state, kind)));
        Optional<Float> alphaTest = alphaTest(state, kind);
        if (alphaTest.isPresent()) {
            builder = builder.withShaderDefine("ALPHA_CUTOUT", alphaTest.get());
        }
        if (state.lighting() != BrRenderState.LightingModel.DIRECTIONAL) {
            // vanilla entity.vsh 原生编译期分支：vertexColor = Color，去掉法线方向光（ADR-0033）
            builder = builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        }
        if (state.stencil().isPresent()) {
            builder = builder.withStencilTest(buildStencilTest(state.stencil().get()));
        }
        RenderPipeline pipeline = builder.build();
        // Iris 26.1 按 RenderPipeline 对象身份匹配光影程序，未登记的 pipeline 会用原版程序绘制
        // （不写 G-buffer：整片发黑/不投影/近景大面变黑）。构建期用官方 API 登记主通道与阴影通道。
        IrisPipelineCompat.assign(pipeline, state);
        return pipeline;
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

    private static DepthStencilState buildDepthStencilState(BrRenderState state, PassKind kind) {
        CompareOp func = state.depth().test()
                ? state.depth().func().map(BrRenderTypeFactory::mapCompareOp).orElse(CompareOp.LESS_THAN_OR_EQUAL)
                : CompareOp.ALWAYS_PASS;
        return new DepthStencilState(func, effectiveWriteDepth(state, kind));
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

    public static RenderType createForEntityPass(PortResourceLocation texture,
                                                 PortRenderPass.Transparency transparency,
                                                 boolean disableCulling) {
        BrRenderState state = canonicalEntityState(transparency, disableCulling);
        return CACHE.computeIfAbsent(new Key(texture, state, PassKind.ENTITY),
                key -> custom(ResourceLocationBridge.toMc(texture), state, PassKind.ENTITY));
    }

    /**
     * canonical {@link PortRenderPass}（状态只有透明度 + cull 两维）→ 实体通道 {@link BrRenderState}。
     *
     * <p>补全的维度口径必须与 vanilla 26.1 同名 pipeline 一致，否则「材质驱动」与「回退 RenderType」
     * 两条路径会给同一材质不同的深度/剔除行为（历史上就是 26.1 半透明不写深度、且 cull 丢失的根源）：
     * <ul>
     *   <li>深度写：solid / cutout / translucent 写；translucent_emissive 不写
     *       （vanilla ENTITY_TRANSLUCENT_EMISSIVE = {@code DepthStencilState(LESS_THAN_OR_EQUAL, false)}）。</li>
     *   <li>混合：additive 用 SRC_ALPHA/ONE（Bedrock additive 语义），其余用标准 alpha 混合。</li>
     * </ul>
     */
    private static BrRenderState canonicalEntityState(PortRenderPass.Transparency transparency, boolean disableCulling) {
        BrRenderState.Transparency t = switch (transparency) {
            case SOLID -> BrRenderState.Transparency.NONE;
            case ALPHA_TEST -> BrRenderState.Transparency.ALPHA_TEST;
            case TRANSLUCENT, TRANSLUCENT_EMISSIVE -> BrRenderState.Transparency.BLEND;
            case ADDITIVE -> BrRenderState.Transparency.ADDITIVE;
        };
        BrRenderState.SurfaceClass sc = switch (transparency) {
            case SOLID -> BrRenderState.SurfaceClass.OPAQUE;
            case ALPHA_TEST -> BrRenderState.SurfaceClass.CUTOUT;
            case TRANSLUCENT -> BrRenderState.SurfaceClass.TRANSLUCENT;
            case TRANSLUCENT_EMISSIVE -> BrRenderState.SurfaceClass.TRANSLUCENT_EMISSIVE;
            case ADDITIVE -> BrRenderState.SurfaceClass.ADDITIVE;
        };
        Optional<BrRenderState.Blend> blend = switch (transparency) {
            case ADDITIVE -> Optional.of(new BrRenderState.Blend(
                    BlendFactor.SourceAlpha, BlendFactor.One, BlendFactor.One, BlendFactor.OneMinusSrcAlpha));
            case TRANSLUCENT, TRANSLUCENT_EMISSIVE -> Optional.of(new BrRenderState.Blend(
                    BlendFactor.SourceAlpha, BlendFactor.OneMinusSrcAlpha,
                    BlendFactor.One, BlendFactor.OneMinusSrcAlpha));
            case SOLID, ALPHA_TEST -> Optional.empty();
        };
        return new BrRenderState(
                sc,
                !disableCulling,
                t,
                new BrRenderState.Depth(true, Optional.empty()),
                new BrRenderState.WriteMask(true, transparency != PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE),
                blend,
                Optional.empty(),
                true,
                true,
                java.util.Set.of(),
                false,
                transparency == PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE,
                BrRenderState.LightingModel.LIGHTMAP_ONLY);
    }

    private record Key(PortResourceLocation texture, BrRenderState state, PassKind kind) {
    }

    //? if <26.1 {
    //?} else {
    private record PipelineKey(BrRenderState state, PassKind kind) {
    }
    //?}
}


