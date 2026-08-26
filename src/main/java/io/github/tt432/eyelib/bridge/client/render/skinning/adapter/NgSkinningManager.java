//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.bridge.event.ManagerReplacedEventPublisher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * C1 GPU 蒙皮管理器（26.1.2，ADR-0032）：骨骼调色板 ring buffer、蒙皮管线派生缓存、
 * 静态几何缓存、逐帧绘制记录的聚合与阶段 flush。
 *
 * <p>flush 时机（世界路径）：{@code AfterOpaqueFeatures}（在 vanilla 半透明目标深度拷贝之前，
 * 保持「不透明实体参与半透明遮挡」语义）与 {@code AfterTranslucentFeatures}（半透明地形之前）。
 * 非世界路径（物品栏 PIP / FBO 场景，{@code RenderSystem.outputColorTextureOverride} 非空）
 * 由 {@code NgSkinningSession.finish} 立即执行，不经队列。
 *
 * <p>开关：{@code -Deyelib.gpuSkinning=false} 关闭（回到经典 CPU 蒙皮）。
 */
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
public final class NgSkinningManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NgSkinningManager.class);
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("eyelib.gpuSkinning", "true"));
    private static final int RING_CAPACITY = 8 << 20;

    private static final IdentityHashMap<BakedModel, SkinnedGeometry> GEOMETRIES = new IdentityHashMap<>();
    private static final IdentityHashMap<RenderPipeline, RenderPipeline> PIPELINES = new IdentityHashMap<>();
    private static final List<NgSkinningSession> SOLID_DRAWS = new ArrayList<>();
    private static final List<NgSkinningSession> TRANSLUCENT_DRAWS = new ArrayList<>();
    private static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final List<String> PALETTE_DYNAMIC_UNIFORMS = List.of("BonePalette");
    private static final Vector3f ZERO = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private static @Nullable MappableRingBuffer paletteRing;
    private static int ringOffset;
    private static java.nio.@Nullable ByteBuffer paletteStaging;
    private static int alignment = 256;
    private static boolean hooksInstalled;
    private static boolean warnedRingFull;
    private static boolean warnedLeftover;

    private NgSkinningManager() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    /** DeferredRenderSink 入口：创建一次提交的蒙皮会话；关闭时返回 null（走经典路径）。 */
    public static @Nullable NgSkinningSession createSession(RenderType routingType, Identifier texture) {
        if (!ENABLED) {
            return null;
        }
        installInvalidationHooks();
        return new NgSkinningSession(routingType, texture);
    }

    /** 几何缓存（按 BakedModel 实例身份；模型失效时整体清空）。 */
    static @Nullable SkinnedGeometry geometry(BakedModel model) {
        synchronized (GEOMETRIES) {
            SkinnedGeometry cached = GEOMETRIES.get(model);
            if (cached != null) {
                return cached;
            }
        }
        // 创建放锁外：createBuffer 可能触发驱动同步；并发创建同模型几何的最坏结果是多建一份，
        // putIfAbsent 失败方立即 close，不影响正确性。
        SkinnedGeometry created = SkinnedGeometry.create(model);
        if (created == null) {
            return null;
        }
        synchronized (GEOMETRIES) {
            SkinnedGeometry raced = GEOMETRIES.putIfAbsent(model, created);
            if (raced != null) {
                created.close();
                return raced;
            }
        }
        return created;
    }

    /** 蒙皮管线派生：routing 管线（材质状态单一事实源）换蒙皮 VS + 蒙皮顶点格式 + BonePalette UBO。 */
    static RenderPipeline derivePipeline(RenderPipeline routing) {
        synchronized (PIPELINES) {
            return PIPELINES.computeIfAbsent(routing, p -> p.toBuilder()
                    .withLocation(Identifier.parse("eyelib:skinning/" + Integer.toHexString(System.identityHashCode(p))))
                    .withVertexShader(Identifier.parse("eyelib:core/entity_skinned"))
                    .withVertexFormat(SkinnedGeometry.FORMAT, VertexFormat.Mode.TRIANGLES)
                    .withUniform("BonePalette", UniformType.UNIFORM_BUFFER)
                    .withShaderDefine("MAX_BONES", SkinningLayout.MAX_BONES)
                    .build());
        }
    }

    /**
     * 分配当帧 palette block（offset 按 UBO 对齐），返回 ring 内偏移。
     * 返回 -1 = 当帧容量耗尽，调用方回退经典路径。
     *
     * <p>会话矩阵写入共享 CPU 暂存（{@link #paletteStaging}，返回值即其内偏移），
     * GPU 上传在 flush 时一次性完成（每 phase 一次 mapBuffer + 批量拷贝，
     * 取代逐实体 mapBuffer——后者是 n384 实测的主要开销项）。
     */
    static synchronized int acquirePalette(int size) {
        MappableRingBuffer ring = paletteRing;
        if (ring == null) {
            alignment = RenderSystem.getDevice().getUniformOffsetAlignment();
            paletteRing = ring = new MappableRingBuffer(() -> "eyelib-bone-palette",
                    GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM, RING_CAPACITY);
            paletteStaging = java.nio.ByteBuffer.allocateDirect(RING_CAPACITY)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }
        int offset = (ringOffset + alignment - 1) / alignment * alignment;
        if (offset + size > RING_CAPACITY) {
            if (!warnedRingFull) {
                warnedRingFull = true;
                LOGGER.error("[skinning] bone palette ring exhausted ({} bytes/frame needed so far); "
                        + "remaining entities this frame fall back to CPU skinning", ringOffset);
            }
            return -1;
        }
        ringOffset = offset + size;
        return offset;
    }

    /** 会话矩阵写入目标（容量 = ring 容量；调用方按 acquirePalette 返回的 offset 定位）。 */
    static java.nio.ByteBuffer paletteStaging() {
        var staging = paletteStaging;
        if (staging == null) {
            throw new IllegalStateException("acquirePalette must be called first");
        }
        return staging;
    }

    /** acquire 返回的 offset → 当帧 UBO slice（finish 时调用，同一帧的 currentBuffer）。 */
    static GpuBufferSlice paletteSlice(int offset, int size) {
        var ring = paletteRing;
        if (ring == null) {
            throw new IllegalStateException("acquirePalette must be called first");
        }
        return ring.currentBuffer().slice(offset, size);
    }

    /** 把 [0, uptoOffset) 的暂存内容一次性写入 ring（每 phase flush 一次）。 */
    private static void uploadPalette(int uptoOffset) {
        var ring = paletteRing;
        var staging = paletteStaging;
        if (ring == null || staging == null || uptoOffset <= 0) {
            return;
        }
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(ring.currentBuffer().slice(0, uptoOffset), false, true)) {
            view.data().put(0, staging, 0, uptoOffset);
        }
    }

    /** 会话完成：override 目标（PIP/物品栏/FBO）立即绘制，否则入世界路径阶段队列。 */
    static void submitDraw(NgSkinningSession session) {
        if (RenderSystem.outputColorTextureOverride != null) {
            execute(List.of(session), RenderSystem.getModelViewMatrix());
        } else if (session.isTranslucent()) {
            TRANSLUCENT_DRAWS.add(session);
        } else {
            SOLID_DRAWS.add(session);
        }
    }

    @SubscribeEvent
    public static void onFramePre(RenderFrameEvent.Pre event) {
        ringOffset = 0;
        if (paletteRing != null) {
            paletteRing.rotate();
        }
        if (!SOLID_DRAWS.isEmpty() || !TRANSLUCENT_DRAWS.isEmpty()) {
            if (!warnedLeftover) {
                warnedLeftover = true;
                LOGGER.warn("[skinning] {} solid + {} translucent draw records leaked across frame boundary; dropping "
                        + "(writers ran outside LevelRenderer stage window without output override)",
                        SOLID_DRAWS.size(), TRANSLUCENT_DRAWS.size());
            }
            SOLID_DRAWS.clear();
            TRANSLUCENT_DRAWS.clear();
        }
    }

    @SubscribeEvent
    public static void onAfterOpaqueFeatures(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        flush(SOLID_DRAWS, event.getModelViewMatrix());
    }

    @SubscribeEvent
    public static void onAfterTranslucentFeatures(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        flush(TRANSLUCENT_DRAWS, event.getModelViewMatrix());
    }

    private static void flush(List<NgSkinningSession> records, Matrix4fc modelView) {
        if (records.isEmpty()) {
            return;
        }
        List<NgSkinningSession> batch = new ArrayList<>(records);
        records.clear();
        execute(batch, modelView);
    }

    /**
     * 执行一批蒙皮绘制：按 (pipeline, texture) 连续分组，每组一个 RenderPass。
     * 顺序即 writer 回调顺序（上游 passOrder 已排序），跨组状态切换才开新 pass。
     */
    private static void execute(List<NgSkinningSession> records, Matrix4fc modelView) {
        Minecraft mc = Minecraft.getInstance();
        GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride
                : mc.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride
                : mc.getMainRenderTarget().getDepthTextureView();
        if (color == null) {
            LOGGER.warn("[skinning] no color target available; dropping {} draw records", records.size());
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        // mapBuffer（writeTransform/uploadPalette 内部）禁止在 RenderPass 开启期间调用，先上传再建 pass
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, WHITE, ZERO, IDENTITY);
        uploadPalette(ringOffset);
        int i = 0;
        while (i < records.size()) {
            RenderPipeline pipeline = records.get(i).pipeline();
            Identifier texture = records.get(i).texture();
            int j = i + 1;
            while (j < records.size()
                    && records.get(j).pipeline() == pipeline
                    && records.get(j).texture().equals(texture)) {
                j++;
            }
            // 纹理解析可能触发懒上传（TextureManager.getTexture → DynamicTexture.upload
            // → writeToTexture），禁止在 RenderPass 开启期间执行——与 vanilla
            // RenderType.draw 同模式（state.getTextures() 在 createRenderPass 之前）
            AbstractTexture tex = mc.getTextureManager().getTexture(texture);
            GpuTextureView textureView = tex.getTextureView();
            GpuSampler textureSampler = tex.getSampler();
            GpuTextureView overlayView = mc.gameRenderer.overlayTexture().getTextureView();
            GpuTextureView lightmapView = mc.gameRenderer.lightmap();
            // vanilla chunk 渲染同构（ChunkSectionsToRender.renderGroup）：一次 drawMultipleIndexed
            // 提交组内全部实体/可见区间，per-draw 经 uniformUploaderConsumer 绑定各自 palette slice
            List<RenderPass.Draw<GpuBufferSlice>> draws = new ArrayList<>(records.size());
            for (int k = i; k < j; k++) {
                NgSkinningSession rec = records.get(k);
                SkinnedGeometry geo = rec.geometry();
                GpuBufferSlice palette = rec.paletteSlice();
                int[] ranges = rec.ranges();
                for (int r = 0; r < ranges.length; r += 2) {
                    draws.add(new RenderPass.Draw<>(0, geo.vertexBuffer(), geo.indexBuffer(),
                            VertexFormat.IndexType.INT, ranges[r], ranges[r + 1], 0,
                            (unused, uploader) -> uploader.upload("BonePalette", palette)));
                }
                // 同一实体的多个区间共享其 palette slice：闭包按 Draw 捕获（uniformArgument 恒 null）
            }
            try (RenderPass pass = encoder.createRenderPass(() -> "eyelib skinned entities",
                    color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                bindSamplers(pass, pipeline, textureView, textureSampler, overlayView, lightmapView);
                pass.drawMultipleIndexed(draws, null, null, PALETTE_DYNAMIC_UNIFORMS, null);
            }
            i = j;
        }
    }

    private static void bindSamplers(RenderPass pass, RenderPipeline pipeline, GpuTextureView textureView, GpuSampler textureSampler,
                                     GpuTextureView overlayView, GpuTextureView lightmapView) {
        var samplerCache = RenderSystem.getSamplerCache();
        for (String sampler : pipeline.getSamplers()) {
            switch (sampler) {
                case "Sampler0" -> pass.bindTexture("Sampler0", textureView, textureSampler);
                case "Sampler1" -> pass.bindTexture("Sampler1", overlayView,
                        samplerCache.getClampToEdge(FilterMode.LINEAR));
                case "Sampler2" -> pass.bindTexture("Sampler2", lightmapView,
                        samplerCache.getClampToEdge(FilterMode.LINEAR));
                default -> LOGGER.warn("[skinning] pipeline {} declares unsupported sampler {}", pipeline.getLocation(), sampler);
            }
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
            GEOMETRIES.values().forEach(SkinnedGeometry::close);
            GEOMETRIES.clear();
        }
    }
}
//?}
