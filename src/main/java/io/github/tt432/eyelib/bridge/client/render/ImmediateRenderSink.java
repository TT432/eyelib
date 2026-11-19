//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
//? if <1.20.6 {
import io.github.tt432.eyelib.bridge.material.adapter.TextureColorMaterial;
import io.github.tt432.eyelib.bridge.client.render.adapter.TextureColorWorldPass;
import com.mojang.blaze3d.vertex.BufferUploader;
import org.lwjgl.system.MemoryUtil;
//?} else {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
//?}
import io.github.tt432.eyelib.material.port.PassTier;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link RenderSink} 的立即实现（1.20.1 / 1.21.1）。
 *
 * <p>{@code submit} 同步从 {@link MultiBufferSource} 取 VertexConsumer 并执行 writer；
 * {@code flush} 在底层是 {@link MultiBufferSource.BufferSource} 时执行 {@code endBatch()} 触发立即绘制。
 * 行为与改造前的 getBuffer + 写 + endBatch 一致。
 *
 * <p>合批（{@link #openBatch}）：批次是 sink 实例状态（{@code batchDepth} + {@code policy}），
 * 最外层策略生效；嵌套策略忽略并 WARN 一次。
 * <ul>
 *   <li>作用域内 {@link #flush()} 只表达「调用方希望落屏」，<b>不绘制</b>；作用域外行为与不开批次一致。</li>
 *   <li>{@link BatchPolicy#mergeDraws()} 为 true 时自建 {@link LinkedHashMap}&lt;RenderType, Bucket&gt;，
 *       close 时按 {@link PassTier} 分组（不透明先、混合后）、组内按首次提交顺序各 draw 一次。</li>
 *   <li>关闭顺序：OPAQUE 桶 → OPAQUE 蒙皮会话 → BLEND 桶 → BLEND 蒙皮会话 → 最后
 *       vanilla {@code endBatch()}（vanilla 通道装的是挂载物/手持物/栓绳直写几何）。</li>
 *   <li>{@link BatchPolicy#allowGpuSkinning()} 为 false 时向 writer 传 {@code skinning=null}。</li>
 *   <li>堆外资源：close 与批次外的 flush 都释放桶（1.20.1 的 {@code BufferBuilder} 用 native malloc，
 *       1.21.1 的 {@code ByteBufferBuilder} 需 close）。</li>
 * </ul>
 *
 * @author TT432
 */
@SuppressWarnings("PMD.LawOfDemeter")
final class ImmediateRenderSink implements RenderSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImmediateRenderSink.class);
    /** 桶初始容量（字节）：实体模型通常远超此值，交给底层缓冲自行增长。 */
    private static final int BUCKET_CAPACITY = 1024;

    private final MultiBufferSource bufferSource;
    /** 本实体已完成的蒙皮会话（绘制见 flush / 批次 close）。 */
    private final List<SkinnedDraw> skinnedDraws = new ArrayList<>();
    /** mergeDraws 批次的几何桶：按 RenderType 实例分组，保留首次提交顺序。 */
    private final Map<RenderType, Bucket> buckets = new LinkedHashMap<>();
    /** 累计出现过的 RenderType 实例（identity，用于评估合批率）。 */
    private final Set<RenderType> seenRenderTypes = Collections.newSetFromMap(new IdentityHashMap<>());

    /** 批次深度；>0 表示处于批次作用域内。 */
    private int batchDepth;
    /** 最外层 openBatch 的策略，批次结束后复位为 {@link BatchPolicy#DEFER}。 */
    private BatchPolicy policy = BatchPolicy.DEFER;
    private boolean nestedPolicyWarned;
    private boolean unclosedBatchWarned;

    private int submits;
    private int skinnedSubmits;
    private int draws;
    private int vanillaEndBatches;

    ImmediateRenderSink(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public void submit(PortRenderPass renderPass, PortResourceLocation texture,
                       PoseStack pose, GeometryWriter writer) {
        RenderType renderType = MaterialPort.toRenderType(renderPass, texture);
        submits++;
        seenRenderTypes.add(renderType);
        //? if <1.20.6 {
        if (TextureColorMaterial.contains(renderType)) {
            if (io.github.tt432.eyelib.bridge.client.compat.oculus.OculusCompat.renderingShadowPass()) return;
            if (TextureColorWorldPass.collecting()) {
                // 原色世界迟绘队列有独立的缓冲与提交时机：直接写它的 buffer，不进桶。
                writer.write(pose.last(), TextureColorWorldPass.buffer(renderType), null);
                return;
            }
        }
        //?}
        boolean merging = batching() && policy.mergeDraws();
        VertexConsumer consumer = merging
                ? buckets.computeIfAbsent(renderType, type -> new Bucket(type, renderPass.tier())).consumer()
                : bufferSource.getBuffer(renderType);
        // allowGpuSkinning=false：蒙皮会话只采集 palette、不向 consumer 写顶点，合并模式下桶会空，
        // 因此必须不建会话，让 writer 把顶点写进批次缓冲（C1 GPU 蒙皮被批次临时禁用）。
        LegacySkinningSession skinning = batching() && !policy.allowGpuSkinning()
                ? null
                : LegacySkinningManager.createSession(renderType);
        if (skinning != null) {
            skinnedSubmits++;
        }
        try {
            writer.write(pose.last(), consumer, skinning);
        } catch (RuntimeException e) {
            if (skinning != null) {
                skinning.discard();
            }
            throw e;
        }
        if (skinning != null) {
            skinning.finish();
            if (skinning.hasDraw()) {
                skinnedDraws.add(new SkinnedDraw(renderPass.tier(), skinning));
            }
        }
    }

    @Override
    public void flush() {
        if (batching()) {
            // 批次内：绘制统一由最外层 close 完成，此处只吞掉调用方的 flush 意图。
            return;
        }
        if (!buckets.isEmpty()) {
            // 兜底：桶只在 mergeDraws 批次内产生，走到这里说明调用方漏了 close。
            warnUnclosedBatch();
            releaseBuckets();
        }
        endVanillaBatch();
        // 蒙皮绘制在 endBatch 之后：透明组件经 passOrder 排序后最后 flush，
        // 其蒙皮部分自然排在全部缓冲几何之后（与 26.1.2 半透明后画语义一致）
        for (SkinnedDraw draw : skinnedDraws) {
            LegacySkinningManager.draw(draw.session());
            draws++;
        }
        skinnedDraws.clear();
    }

    @Override
    public BatchScope openBatch(BatchPolicy policy) {
        if (batchDepth > 0) {
            if (!nestedPolicyWarned) {
                nestedPolicyWarned = true;
                LOGGER.warn("[renderBatch] 嵌套 openBatch 的策略 {} 被忽略：批次策略由最外层 openBatch 决定",
                        policy);
            }
            batchDepth++;
            return new Scope(false);
        }
        this.policy = policy;
        batchDepth = 1;
        return new Scope(true);
    }

    @Override
    public FlushMode flushMode() {
        return FlushMode.PER_BATCH;
    }

    @Override
    public SkinningSupport skinningSupport(PortRenderPass pass, PortResourceLocation texture) {
        if (batching() && !policy.allowGpuSkinning()) {
            return SkinningSupport.DISABLED_BY_BATCH;
        }
        // 必须用同一个 RenderType 实例查询：LegacySkinningManager.VARIANTS 是 IdentityHashMap。
        return LegacySkinningManager.support(MaterialPort.toRenderType(pass, texture));
    }

    @Override
    public int submitCount() {
        return submits;
    }

    @Override
    public MultiBufferSource multiBufferSource() {
        return bufferSource;
    }

    private boolean batching() {
        return batchDepth > 0;
    }

    private BatchStats stats() {
        return new BatchStats(submits, skinnedSubmits, seenRenderTypes.size(), draws, vanillaEndBatches);
    }

    private void endVanillaBatch() {
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
            vanillaEndBatches++;
        }
    }

    /** 最外层 close：按档位绘制桶与蒙皮会话，最后刷 vanilla 通道。 */
    private void finishBatch() {
        try {
            drawBuckets(PassTier.OPAQUE);
            drawSkinned(PassTier.OPAQUE);
            drawBuckets(PassTier.BLEND);
            drawSkinned(PassTier.BLEND);
            // vanilla 通道里是挂载物/手持物/栓绳的直写几何：必须在模型几何之后刷出。
            endVanillaBatch();
        } finally {
            releaseBuckets();
            skinnedDraws.clear();
            batchDepth = 0;
            policy = BatchPolicy.DEFER;
        }
    }

    private void drawBuckets(PassTier tier) {
        for (Bucket bucket : buckets.values()) {
            if (bucket.tier() != tier) continue;
            if (bucket.draw()) {
                draws++;
            }
        }
    }

    private void drawSkinned(PassTier tier) {
        for (SkinnedDraw draw : skinnedDraws) {
            if (draw.tier() != tier) continue;
            LegacySkinningManager.draw(draw.session());
            draws++;
        }
    }

    private void releaseBuckets() {
        for (Bucket bucket : buckets.values()) {
            bucket.release();
        }
        buckets.clear();
    }

    private void warnUnclosedBatch() {
        if (unclosedBatchWarned) return;
        unclosedBatchWarned = true;
        LOGGER.warn("[renderBatch] 批次未 close 就 flush：{} 个几何桶被丢弃（openBatch 必须 try-with-resources）",
                buckets.size());
    }

    /** 一次待绘制（或已绘制）的蒙皮提交。 */
    private record SkinnedDraw(PassTier tier, LegacySkinningSession session) {
    }

    /**
     * 批次作用域：close 幂等，只有最外层 close 真正绘制。
     */
    private final class Scope implements BatchScope {
        private final boolean outermost;
        private boolean closed;

        Scope(boolean outermost) {
            this.outermost = outermost;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (batchDepth > 0) {
                batchDepth--;
            }
            if (outermost) {
                finishBatch();
            }
        }

        @Override
        public BatchStats stats() {
            return ImmediateRenderSink.this.stats();
        }

        @Override
        public boolean isOpen() {
            return !closed && batchDepth > 0;
        }
    }

    //? if <1.20.6 {
    /**
     * 1.20.1 几何桶：自建 {@link BufferBuilder}（native malloc），关闭时镜像
     * {@code MultiBufferSource.BufferSource#endBatch(RenderType)}——{@code endOrDiscardIfEmpty} +
     * {@code setupRenderState}/{@code drawWithShader}/{@code clearRenderState}。
     */
    private static final class Bucket {
        private final RenderType renderType;
        private final PassTier tier;
        private final BufferBuilder buffer;

        Bucket(RenderType renderType, PassTier tier) {
            this.renderType = renderType;
            this.tier = tier;
            this.buffer = new BufferBuilder(BUCKET_CAPACITY);
            this.buffer.begin(renderType.mode(), renderType.format());
        }

        PassTier tier() {
            return tier;
        }

        VertexConsumer consumer() {
            return buffer;
        }

        boolean draw() {
            if (!buffer.building()) return false;
            // 1.20.1 的 RenderType.sortOnUpload 是 private：按档位决定上传前排序
            // （混合必须远到近；不透明排序纯浪费算力）。
            if (tier == PassTier.BLEND) {
                buffer.setQuadSorting(RenderSystem.getVertexSorting());
            }
            BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
            if (rendered == null) return false;
            renderType.setupRenderState();
            try {
                // upload 内部对空缓冲调用 release()，非空由 VertexBuffer.upload 释放。
                BufferUploader.drawWithShader(rendered);
            } finally {
                renderType.clearRenderState();
            }
            return true;
        }

        void release() {
            buffer.discard();
            // 1.20.1 BufferBuilder 用 native malloc：只丢 Java 对象不会释放堆外内存。
            MemoryUtil.memFree(buffer.buffer);
        }
    }
    //?} else {
    /**
     * 1.21.1 几何桶：自有 {@link ByteBufferBuilder} + {@link BufferBuilder}，关闭时镜像
     * {@code BufferSource#endBatch(RenderType)}——{@code build()} + {@code sortOnUpload()} 排序 +
     * {@code RenderType#draw}（draw 内部 upload 会 close 掉 MeshData，释放 ByteBufferBuilder.Result）。
     */
    private static final class Bucket {
        private final RenderType renderType;
        private final PassTier tier;
        private final ByteBufferBuilder byteBuffer = new ByteBufferBuilder(BUCKET_CAPACITY);
        private final BufferBuilder buffer;

        Bucket(RenderType renderType, PassTier tier) {
            this.renderType = renderType;
            this.tier = tier;
            this.buffer = new BufferBuilder(byteBuffer, renderType.mode(), renderType.format());
        }

        PassTier tier() {
            return tier;
        }

        VertexConsumer consumer() {
            return buffer;
        }

        boolean draw() {
            MeshData mesh = buffer.build();
            if (mesh == null) return false;
            if (renderType.sortOnUpload()) {
                mesh.sortQuads(byteBuffer, RenderSystem.getVertexSorting());
            }
            renderType.draw(mesh);
            return true;
        }

        void release() {
            byteBuffer.close();
        }
    }
    //?}
}
//?}
