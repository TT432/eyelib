//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.bridge.event.ManagerReplacedEventPublisher;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;

//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
//?}

/**
 * C1 GPU 蒙皮管理器（&le;26.1，ADR-0032）：蒙皮 ShaderInstance 注册（RegisterShadersEvent）、
 * routing RenderType → 蒙皮变体映射（BrRenderTypeFactory 创建期登记）、静态几何缓存、
 * palette 暂存池，以及单次绘制的执行。
 *
 * <p>两条 GPU 路径（{@link Mode}）：GL&ge;4.6 时跨实体合批 compute 蒙皮（skin_batch.comp
 * 写全局输出 SSBO + 按 RenderType 组合批直通绘制，见 {@code BatchSkinningDispatcher}），
 * 否则 VS 蒙皮（uniform 调色板 + 蒙皮 vsh）；二者都不可用时整体回退经典 CPU 蒙皮。
 * {@code -Deyelib.gpuSkinning.mode=vs|compute|auto}（默认 auto）可强制路径用于基准对照。
 *
 * <p>绘制时机：VS 路径在 {@code ImmediateRenderSink.flush}（实体渲染调用栈内，等价 vanilla
 * endBatch 时机）逐会话绘制；compute 路径窗口内延迟至 AFTER_ENTITIES 合批（窗口外立即 drain）。
 * overlay/lightmap 纹理由 routing RenderType 的 OverlayStateShard/LightmapStateShard 在
 * setupRenderState 内绑定（Sampler1/Sampler2），Sampler0 由其 TextureStateShard 绑定。
 *
 * <p>开关：{@code -Deyelib.gpuSkinning=false} 关闭（回到经典 CPU 蒙皮）。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class LegacySkinningManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySkinningManager.class);
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("eyelib.gpuSkinning", "true"));

    /** 蒙皮路径；{@code -Deyelib.gpuSkinning.mode} 控制（auto = compute 优先，VS 兜底）。 */
    public enum Mode {
        OFF,
        VERTEX_SHADER,
        COMPUTE;

        static Mode requested() {
            return switch (System.getProperty("eyelib.gpuSkinning.mode", "auto")) {
                case "vs" -> VERTEX_SHADER;
                case "compute" -> COMPUTE;
                default -> null; // auto
            };
        }
    }

    /** 蒙皮变体（shader 数组下标）；UNSUPPORTED = 不蒙皮（回退 CPU）。 */
    public static final int VARIANT_UNSUPPORTED = -1;
    public static final int VARIANT_CUTOUT = 0;
    public static final int VARIANT_SOLID = 1;
    public static final int VARIANT_TRANSLUCENT = 2;
    public static final int VARIANT_EMISSIVE = 3;

    // 1.20.1 与 1.21.1 的 vanilla 蒙皮母版不同（1.21.1 fog_distance 改 2 参签名、去 IViewRotMat/normal 输出），
    // 资产与命名都按版本分开；FS 均复用 vanilla 同名 fsh
    //? if <1.20.6 {
    private static final String[] VS_SHADER_NAMES = {
            "entity_skinned_legacy",
            "entity_skinned_legacy_solid",
            "entity_skinned_legacy_translucent",
            "entity_skinned_legacy_emissive"
    };
    private static final String[] COMPUTE_SHADER_NAMES = {
            "entity_skinned_batch",
            "entity_skinned_batch_solid",
            "entity_skinned_batch_translucent",
            "entity_skinned_batch_emissive"
    };
    //?} else {
    private static final String[] VS_SHADER_NAMES = {
            "entity_skinned_legacy_121",
            "entity_skinned_legacy_121_solid",
            "entity_skinned_legacy_121_translucent",
            "entity_skinned_legacy_emissive_121"
    };
    private static final String[] COMPUTE_SHADER_NAMES = {
            "entity_skinned_batch_121",
            "entity_skinned_batch_121_solid",
            "entity_skinned_batch_121_translucent",
            "entity_skinned_batch_emissive_121"
    };
    //?}

    /** GL_MAX_VERTEX_UNIFORM_COMPONENTS（GL 3.2 spec 下限 1024；96 骨骼×2 矩阵+vanilla 套装需 ~3136）。 */
    private static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    private static final int REQUIRED_UNIFORM_COMPONENTS = SkinningLayout.MAX_BONES * 32 + 64;

    private static final ShaderInstance[] VS_SHADERS = new ShaderInstance[VS_SHADER_NAMES.length];
    private static final ShaderInstance[] COMPUTE_SHADERS = new ShaderInstance[COMPUTE_SHADER_NAMES.length];
    private static final IdentityHashMap<RenderType, Integer> VARIANTS = new IdentityHashMap<>();
    private static final IdentityHashMap<BakedModel, LegacyGpuGeometry> GEOMETRIES = new IdentityHashMap<>();
    private static final Deque<float[]> ARRAY_POOL = new ArrayDeque<>();
    private static @Nullable BatchSkinningProgram batchProgram;
    private static Mode mode = Mode.OFF;
    private static boolean hooksInstalled;

    private LegacySkinningManager() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static Mode mode() {
        return mode;
    }

    /** BrRenderTypeFactory.custom 创建期登记 routing RenderType 的蒙皮变体。 */
    public static void registerVariant(RenderType renderType, int variant) {
        synchronized (VARIANTS) {
            VARIANTS.put(renderType, variant);
        }
    }

    /** ImmediateRenderSink 入口：创建一次提交的蒙皮会话；关闭/未知变体/重载窗口 → null（经典路径）。 */
    public static @Nullable LegacySkinningSession createSession(RenderType routingType) {
        if (!ENABLED || mode == Mode.OFF) {
            return null;
        }
        int variant;
        synchronized (VARIANTS) {
            Integer v = VARIANTS.get(routingType);
            if (v == null || v < 0) {
                return null;
            }
            variant = v;
        }
        ShaderInstance[] shaders = mode == Mode.COMPUTE ? COMPUTE_SHADERS : VS_SHADERS;
        if (shaders[variant] == null) {
            return null; // 资源重载窗口期：着色器尚未注册
        }
        if (mode == Mode.COMPUTE && batchProgram == null) {
            return null;
        }
        installInvalidationHooks();
        return new LegacySkinningSession(routingType, variant);
    }

    /** 几何缓存（按 BakedModel 实例身份；模型失效时整体清空）。渲染线程调用。 */
    static @Nullable LegacyGpuGeometry geometry(BakedModel model) {
        synchronized (GEOMETRIES) {
            LegacyGpuGeometry cached = GEOMETRIES.get(model);
            if (cached != null) {
                return cached;
            }
        }
        // 创建放锁外：GPU 缓冲创建/上传可能触发驱动同步；并发创建同模型几何的最坏结果是多建一份，
        // putIfAbsent 失败方立即 close，不影响正确性。
        LegacyGpuGeometry created = mode == Mode.COMPUTE
                ? ComputeSkinnedGeometry.create(model)
                : LegacySkinnedGeometry.create(model);
        if (created == null) {
            return null;
        }
        synchronized (GEOMETRIES) {
            LegacyGpuGeometry raced = GEOMETRIES.putIfAbsent(model, created);
            if (raced != null) {
                created.close();
                return raced;
            }
        }
        return created;
    }

    /** 池化 palette 暂存（16 浮点/骨骼 × MAX_BONES）。 */
    static float[] acquireArray() {
        float[] arr = ARRAY_POOL.pollFirst();
        return arr != null ? arr : new float[SkinningLayout.MAX_BONES * 16];
    }

    /** 归还池化暂存（null 安全）。slot 范围外数据不被读取，归还前无需清零。 */
    static void releaseArrays(float @Nullable [] pose, float @Nullable [] normals) {
        // 池上限防膨胀：n384 峰值 ~800 数组≈19MB 可接受，超过直接丢给 GC
        if (pose != null && ARRAY_POOL.size() < 1024) {
            ARRAY_POOL.offerLast(pose);
        }
        if (normals != null && ARRAY_POOL.size() < 1024) {
            ARRAY_POOL.offerLast(normals);
        }
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        if (!ENABLED) {
            return;
        }
        // 重载清理：vanilla 负责关闭旧 ShaderInstance；compute 程序与着色器引用自行回收
        java.util.Arrays.fill(VS_SHADERS, null);
        java.util.Arrays.fill(COMPUTE_SHADERS, null);
        if (batchProgram != null) {
            batchProgram.close();
            batchProgram = null;
        }

        boolean computeOk = BatchSkinningProgram.supported();
        int components = GlStateManager._getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS);
        boolean vsOk = components >= REQUIRED_UNIFORM_COMPONENTS;
        LOGGER.info("[skinning] MAX_VERTEX_UNIFORM_COMPONENTS = {}（VS 路径需要 ≥ {}）",
                components, REQUIRED_UNIFORM_COMPONENTS);

        Mode selected = pickMode(Mode.requested(), computeOk, vsOk);
        // 注册/编译失败逐级降级：COMPUTE → VERTEX_SHADER → OFF。
        // 绝不能把异常抛出事件总线——RegisterShadersEvent 中断会让 vanilla 着色器全部缺失，客户端必崩（2026-08-27 实证）
        while (selected != Mode.OFF) {
            try {
                registerShadersFor(event, selected);
                if (selected == Mode.COMPUTE) {
                    batchProgram = BatchSkinningProgram.create(event.getResourceProvider());
                }
                break;
            } catch (IOException | RuntimeException e) {
                LOGGER.error("[skinning] {} 路径着色器初始化失败，尝试降级", selected, e);
                java.util.Arrays.fill(VS_SHADERS, null);
                java.util.Arrays.fill(COMPUTE_SHADERS, null);
                if (batchProgram != null) {
                    batchProgram.close();
                    batchProgram = null;
                }
                selected = selected == Mode.COMPUTE && vsOk ? Mode.VERTEX_SHADER : Mode.OFF;
            }
        }
        if (selected != mode) {
            mode = selected;
            // 几何类型跟随路径（VertexBuffer vs SSBO/VAO），路径切换必须整体重建
            clearGeometries();
        }
        LOGGER.info("[skinning] GPU 蒙皮路径 = {}", mode);
    }

    private static void registerShadersFor(RegisterShadersEvent event, Mode targetMode) throws IOException {
        String[] names = targetMode == Mode.COMPUTE ? COMPUTE_SHADER_NAMES : VS_SHADER_NAMES;
        ShaderInstance[] target = targetMode == Mode.COMPUTE ? COMPUTE_SHADERS : VS_SHADERS;
        VertexFormat format = targetMode == Mode.COMPUTE
                ? ComputeSkinnedGeometry.BATCH_FORMAT
                : LegacySkinnedGeometry.FORMAT;
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocationBridge.fromParts("eyelib", names[i]), format),
                    shader -> target[idx] = shader);
        }
    }

    /** 路径选择：master 开关外，compute 需 GL≥4.6（用户需求阈值），VS 需调色板 uniform 容量。 */
    private static Mode pickMode(@Nullable Mode requested, boolean computeOk, boolean vsOk) {
        Mode selected = switch (requested == null ? Mode.COMPUTE : requested) {
            case COMPUTE -> computeOk ? Mode.COMPUTE : (vsOk ? Mode.VERTEX_SHADER : Mode.OFF);
            case VERTEX_SHADER -> vsOk ? Mode.VERTEX_SHADER : Mode.OFF;
            case OFF -> Mode.OFF;
        };
        if (requested == Mode.COMPUTE && !computeOk) {
            LOGGER.warn("[skinning] 强制 compute 路径但 GL<4.6，回退 {}", selected);
        }
        if (selected == Mode.OFF) {
            LOGGER.error("[skinning] GPU 蒙皮禁用：compute 与 VS 路径能力均不足，所有实体回退经典 CPU 蒙皮。");
        }
        return selected;
    }

    /**
     * 提交一次蒙皮绘制：VS 路径立即绘制（routing 状态机 setup → uniform → draw → clear）；
     * compute 路径入队 {@link BatchSkinningDispatcher}，窗口内延迟至 AFTER_ENTITIES 合批 drain，
     * 窗口外立即 drain（等价逐实体绘制）。
     */
    public static void draw(LegacySkinningSession session) {
        if (mode == Mode.COMPUTE) {
            BatchSkinningDispatcher.submit(session);
        } else {
            vsDraw(session);
        }
    }

    /** 合批程序（compute 路径；重载窗口期为 null）。 */
    static @Nullable BatchSkinningProgram batchProgram() {
        return batchProgram;
    }

    /** 合批绘制着色器（compute 路径；重载窗口期为 null）。 */
    static @Nullable ShaderInstance batchShader(int variant) {
        return COMPUTE_SHADERS[variant];
    }

    /** 合批窗口 hook 的快速开关（stage 事件每帧触发，避免无关模式下空转）。 */
    static boolean batchingActive() {
        return ENABLED && mode == Mode.COMPUTE;
    }

    /** benchmark FBO 场景的显式合批窗口（渲染循环前调用；level stage 窗口外的手动渲染用）。 */
    public static void openBatchWindow() {
        if (batchingActive()) {
            BatchSkinningDispatcher.openWindow();
        }
    }

    /** 显式 drain + 关窗（FBO 场景循环结束后、恢复主帧缓冲前调用）。 */
    public static void drainBatch() {
        if (batchingActive()) {
            BatchSkinningDispatcher.drain();
            BatchSkinningDispatcher.closeWindow();
        }
    }

    /** VS 路径：蒙皮 uniform 设置 + VertexBuffer.drawWithShader（自动 MV/Proj/Fog/ColorModulator/Light0/1 + sampler 收集）。 */
    private static void vsDraw(LegacySkinningSession session) {
        ShaderInstance shader = VS_SHADERS[session.variant()];
        if (shader == null || !(session.geometry() instanceof LegacySkinnedGeometry geometry)) {
            session.releaseArrays();
            return; // 重载窗口期：丢一帧，保流程
        }
        RenderType routingType = session.routingType();
        routingType.setupRenderState();
        try {
            setIfPresent(shader, "BonePose", session.poseArray());
            setIfPresent(shader, "BoneNormal", session.normalArray());
            setEntityUniforms(shader, session);

            VertexBuffer buffer = geometry.vertexBuffer();
            buffer.bind();
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        } finally {
            routingType.clearRenderState();
            session.releaseArrays();
        }
    }

    /** tint/overlay/lightmap 逐实体 uniform（VS 路径用；缺失名静默跳过，兼容变体差异）。 */
    private static void setEntityUniforms(ShaderInstance shader, LegacySkinningSession session) {
        setIfPresent(shader, "TintColor", session.tintR(), session.tintG(), session.tintB(), session.tintA());
        setIfPresent(shader, "OverlayUV", session.overlayU(), session.overlayV());
        setIfPresent(shader, "LightUV", session.lightU(), session.lightV());
    }

    private static void setIfPresent(ShaderInstance shader, String name, float[] values) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(values);
        }
    }

    private static void setIfPresent(ShaderInstance shader, String name, float x, float y, float z, float w) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    private static void setIfPresent(ShaderInstance shader, String name, int x, int y) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    /** 模型条目失效/整体替换 → 几何缓存全清（关闭 GPU 缓冲）。资源重载频率低，全清代价可忽略。 */
    private static synchronized void installInvalidationHooks() {
        if (hooksInstalled) {
            return;
        }
        hooksInstalled = true;
        ManagerEntryChangedEventPublisher.addListener(event -> {
            if ("ModelManager".equals(event.getManagerName())) {
                clearGeometries();
            }
        });
        ManagerReplacedEventPublisher.addListener(managerName -> {
            if ("ModelManager".equals(managerName)) {
                clearGeometries();
            }
        });
    }

    private static void clearGeometries() {
        synchronized (GEOMETRIES) {
            GEOMETRIES.values().forEach(LegacyGpuGeometry::close);
            GEOMETRIES.clear();
        }
    }
}
//?}
