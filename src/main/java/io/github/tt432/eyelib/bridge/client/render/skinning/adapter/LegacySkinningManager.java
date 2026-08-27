//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.bridge.event.ManagerReplacedEventPublisher;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * palette float[] 池，以及单次绘制的执行（routing 状态机 setup + 蒙皮 uniform + VertexBuffer.drawWithShader）。
 *
 * <p>绘制时机：{@code ImmediateRenderSink.flush}（实体渲染调用栈内，等价 vanilla endBatch 时机），
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

    /** 蒙皮变体（shader 数组下标）；UNSUPPORTED = 不蒙皮（回退 CPU）。 */
    public static final int VARIANT_UNSUPPORTED = -1;
    public static final int VARIANT_CUTOUT = 0;
    public static final int VARIANT_SOLID = 1;
    public static final int VARIANT_TRANSLUCENT = 2;
    public static final int VARIANT_EMISSIVE = 3;

    // 1.20.1 与 1.21.1 的 vanilla 蒙皮母版不同（1.21.1 fog_distance 改 2 参签名、去 IViewRotMat/normal 输出），
    // 资产与命名都按版本分开；FS 均复用 vanilla 同名 fsh
    //? if <1.20.6 {
    private static final String[] SHADER_NAMES = {
            "entity_skinned_legacy",
            "entity_skinned_legacy_solid",
            "entity_skinned_legacy_translucent",
            "entity_skinned_legacy_emissive"
    };
    //?} else {
    private static final String[] SHADER_NAMES = {
            "entity_skinned_legacy_121",
            "entity_skinned_legacy_121_solid",
            "entity_skinned_legacy_121_translucent",
            "entity_skinned_legacy_emissive_121"
    };
    //?}

    /** GL_MAX_VERTEX_UNIFORM_COMPONENTS（GL 3.2 spec 下限 1024；96 骨骼×2 矩阵+vanilla 套装需 ~3136）。 */
    private static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    private static final int REQUIRED_UNIFORM_COMPONENTS = SkinningLayout.MAX_BONES * 32 + 64;

    private static final ShaderInstance[] SHADERS = new ShaderInstance[SHADER_NAMES.length];
    private static final IdentityHashMap<RenderType, Integer> VARIANTS = new IdentityHashMap<>();
    private static final IdentityHashMap<BakedModel, LegacySkinnedGeometry> GEOMETRIES = new IdentityHashMap<>();
    private static final Deque<float[]> ARRAY_POOL = new ArrayDeque<>();
    private static boolean capable = true;
    private static boolean hooksInstalled;

    private LegacySkinningManager() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    /** BrRenderTypeFactory.custom 创建期登记 routing RenderType 的蒙皮变体。 */
    public static void registerVariant(RenderType renderType, int variant) {
        synchronized (VARIANTS) {
            VARIANTS.put(renderType, variant);
        }
    }

    /** ImmediateRenderSink 入口：创建一次提交的蒙皮会话；关闭/未知变体/重载窗口 → null（经典路径）。 */
    public static @Nullable LegacySkinningSession createSession(RenderType routingType) {
        if (!ENABLED || !capable) {
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
        if (SHADERS[variant] == null) {
            return null; // 资源重载窗口期：着色器尚未注册
        }
        installInvalidationHooks();
        return new LegacySkinningSession(routingType, variant);
    }

    /** 几何缓存（按 BakedModel 实例身份；模型失效时整体清空）。渲染线程调用。 */
    static @Nullable LegacySkinnedGeometry geometry(BakedModel model) {
        synchronized (GEOMETRIES) {
            LegacySkinnedGeometry cached = GEOMETRIES.get(model);
            if (cached != null) {
                return cached;
            }
        }
        // 创建放锁外：VertexBuffer 创建/上传可能触发驱动同步；并发创建同模型几何的最坏结果是多建一份，
        // putIfAbsent 失败方立即 close，不影响正确性。
        LegacySkinnedGeometry created = LegacySkinnedGeometry.create(model);
        if (created == null) {
            return null;
        }
        synchronized (GEOMETRIES) {
            LegacySkinnedGeometry raced = GEOMETRIES.putIfAbsent(model, created);
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
        capable = probeCapability();
        if (!capable) {
            LOGGER.error("[skinning] GPU 蒙皮禁用：MAX_VERTEX_UNIFORM_COMPONENTS 不足（需要 ≥ {}）。"
                    + "所有实体回退经典 CPU 蒙皮。", REQUIRED_UNIFORM_COMPONENTS);
            return;
        }
        for (int i = 0; i < SHADER_NAMES.length; i++) {
            final int idx = i;
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocationBridge.fromParts("eyelib", SHADER_NAMES[i]), LegacySkinnedGeometry.FORMAT),
                        shader -> SHADERS[idx] = shader);
            } catch (IOException e) {
                throw new UncheckedIOException("[skinning] 蒙皮着色器编译失败: " + SHADER_NAMES[idx], e);
            }
        }
    }

    /** 一次性能力探测（blaze3d 包装，非裸 GL；渲染线程=shader 注册时机）。 */
    private static boolean probeCapability() {
        int components = GlStateManager._getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS);
        LOGGER.info("[skinning] MAX_VERTEX_UNIFORM_COMPONENTS = {}（需要 ≥ {}）", components, REQUIRED_UNIFORM_COMPONENTS);
        return components >= REQUIRED_UNIFORM_COMPONENTS;
    }

    /**
     * 执行一次蒙皮绘制：routing 状态机 setup（含 Sampler0/1/2 绑定与混合/深度/cull），
     * 蒙皮 uniform 设置，VertexBuffer.drawWithShader（自动 MV/Proj/Fog/ColorModulator/Light0/1 + sampler 收集）。
     */
    public static void draw(LegacySkinningSession session) {
        ShaderInstance shader = SHADERS[session.variant()];
        if (shader == null) {
            session.releaseArrays();
            return; // 重载窗口期：丢一帧，保流程
        }
        RenderType routingType = session.routingType();
        routingType.setupRenderState();
        try {
            setIfPresent(shader, "BonePose", session.poseArray());
            setIfPresent(shader, "BoneNormal", session.normalArray());
            setIfPresent(shader, "TintColor", session.tintR(), session.tintG(), session.tintB(), session.tintA());
            setIfPresent(shader, "OverlayUV", session.overlayU(), session.overlayV());
            setIfPresent(shader, "LightUV", session.lightU(), session.lightV());

            VertexBuffer buffer = session.geometry().vertexBuffer();
            buffer.bind();
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        } finally {
            routingType.clearRenderState();
            session.releaseArrays();
        }
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
            GEOMETRIES.values().forEach(LegacySkinnedGeometry::close);
            GEOMETRIES.clear();
        }
    }
}
//?}
