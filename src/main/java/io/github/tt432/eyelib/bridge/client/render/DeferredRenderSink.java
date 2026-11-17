package io.github.tt432.eyelib.bridge.client.render;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.NgSkinningManager;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * {@link RenderSink} 的延迟实现（26.1.2）。
 *
 * <p>{@code submit} 经 {@link SubmitNodeCollector#submitCustomGeometry} 注册节点，
 * writer 回调在 {@code renderAllFeatures}（{@code renderSolid}/{@code renderTranslucent}）阶段触发，
 * 直接写入 CustomFeatureRenderer 按 RenderType 分组共享的 BufferBuilder——phase 末由 LevelRenderer
 * {@code endBatch} 统一绘制，同 RenderType 的多实体几何合并为一次 draw call。
 *
 * <p>{@code flush} 为空操作。
 *
 * <p>合批（{@link #openBatch}）：26.1 的绘制完全由 vanilla 阶段（{@code renderAllFeatures}）决定，
 * 本 sink 无自有缓冲，因此批次<b>只做策略 + 统计 + 语义占位</b>：
 * <ul>
 *   <li>{@link BatchPolicy#mergeDraws()} 在这里<b>不改变</b>任何绘制时机或分组——同一 RenderType
 *       的多实体几何本来就由 CustomFeatureRenderer 共享 BufferBuilder 合并，sink 无从也不应再合并。</li>
 *   <li>{@link BatchPolicy#allowGpuSkinning()} 被尊重：false 时 submit 传 {@code skinning=null}
 *       （与 {@link #skinningSupport} 的 {@link SkinningSupport#DISABLED_BY_BATCH} 一致）。</li>
 *   <li>{@link #flush()} 在批次内外都无操作；{@code stats().draws} 与
 *       {@code stats().vanillaEndBatches} 恒为 0。</li>
 * </ul>
 *
 * @author TT432
 */
final class DeferredRenderSink implements RenderSink {
    private static final org.slf4j.Logger BATCH_LOG = org.slf4j.LoggerFactory.getLogger(DeferredRenderSink.class);

    private final SubmitNodeCollector collector;
    /** 累计出现过的 RenderType 实例（identity，用于评估合批率）。 */
    private final java.util.Set<RenderType> seenRenderTypes =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** 批次深度；>0 表示处于批次作用域内。 */
    private int batchDepth;
    /** 最外层 openBatch 的策略，批次结束后复位为 {@link BatchPolicy#DEFER}。 */
    private BatchPolicy policy = BatchPolicy.DEFER;
    private boolean nestedPolicyWarned;
    private int submits;
    private int skinnedSubmits;

    DeferredRenderSink(SubmitNodeCollector collector) {
        this.collector = collector;
    }

    @Override
    public void submit(PortRenderPass renderPass, PortResourceLocation texture,
                       PoseStack pose, GeometryWriter writer) {
        RenderType renderType = MaterialPort.toRenderType(renderPass, texture);
        submits++;
        seenRenderTypes.add(renderType);
        // 策略在 submit 时刻定格：writer 回调延迟到 renderAllFeatures 执行，那时批次可能已关闭。
        boolean allowSkinning = !(batching() && !policy.allowGpuSkinning());
        // 直接写 CustomFeatureRenderer 提供的共享 buffer：vanilla 按 RenderType 分组、
        // 每组共享一个 BufferBuilder，phase 末由 LevelRenderer endBatch 统一绘制——真正的批量路径。
        collector.submitCustomGeometry(pose, renderType, (submitPose, consumer) -> {
            if (Boolean.getBoolean("eyelib.ngRenderProbe")) {
                probeConsumerSwap(renderType, consumer);
            }
            // C1 GPU 蒙皮：会话非空时 writer 可跳过顶点写入（palette 采集），
            // 绘制由会话 finish 接管；routing RenderType 仅承担 phase 归类，共享缓冲保持为空。
            var skinning = allowSkinning
                    ? NgSkinningManager.createSession(renderType, ResourceLocationBridge.toMc(texture))
                    : null;
            if (skinning != null) {
                skinnedSubmits++;
            }
            try {
                writer.write(submitPose, consumer, skinning);
            } catch (IllegalStateException e) {
                if (skinning != null) {
                    skinning.discard();
                }
                // 26.1 已知问题（见 work/feedback.json「Not building!」条目）：
                // 极小概率下 vanilla 共享 BufferBuilder 在写入中途被外部 endBatch，
                // 直接抛出会崩掉整个客户端。此处降级为丢弃本段几何并大声记录
                // （renderType + consumer 身份 + 完整栈），保住其余渲染流程。
                logWriterFailure(renderType, consumer, e);
            }
            if (skinning != null) {
                skinning.finish();
            }
        });
    }

    private static final org.slf4j.Logger PROBE_LOG = org.slf4j.LoggerFactory.getLogger(DeferredRenderSink.class);
    private static final java.util.Map<String, Integer> PROBE_SEEN = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicBoolean WRITER_FAILURE_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static void logWriterFailure(RenderType renderType, VertexConsumer consumer, IllegalStateException e) {
        if (WRITER_FAILURE_LOGGED.compareAndSet(false, true)) {
            PROBE_LOG.error("[nodegraph/RenderSink] geometry write dropped: buffer ended mid-write. " +
                            "renderType={} consumer={}@{} — 完整栈见异常（后续同类事件静默跳过）",
                    renderType, consumer.getClass().getSimpleName(),
                    Integer.toHexString(System.identityHashCode(consumer)), e);
        }
    }

    /** 同一 renderType 文本的 consumer 实例变更时打一条（限频，避免高频 StackWalk）。 */
    private static void probeConsumerSwap(RenderType renderType, VertexConsumer consumer) {
        String key = renderType.toString();
        int hash = System.identityHashCode(consumer);
        Integer prev = PROBE_SEEN.put(key, hash);
        if (prev == null) {
            PROBE_LOG.warn("[renderProbe] first rt={} consumer={}@{}", key, consumer.getClass().getSimpleName(), Integer.toHexString(hash));
        } else if (prev != hash) {
            PROBE_LOG.warn("[renderProbe] SWAP rt={} consumer {}@{} -> {}@{}", key,
                    consumer.getClass().getSimpleName(), Integer.toHexString(prev),
                    consumer.getClass().getSimpleName(), Integer.toHexString(hash));
        }
    }

    @Override
    public void flush() {
        // 无操作：本 sink 不持有可刷出的缓冲。绘制发生在 renderAllFeatures 阶段，
        // 由 vanilla 按 RenderType 分组统一提交（flush 在批次内外都无需做任何事）。
    }

    @Override
    public BatchScope openBatch(BatchPolicy policy) {
        if (batchDepth > 0) {
            if (!nestedPolicyWarned) {
                nestedPolicyWarned = true;
                BATCH_LOG.warn("[renderBatch] 嵌套 openBatch 的策略 {} 被忽略：批次策略由最外层 openBatch 决定",
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
        return FlushMode.PER_SUBMIT;
    }

    @Override
    public SkinningSupport skinningSupport(PortRenderPass pass, PortResourceLocation texture) {
        if (batching() && !policy.allowGpuSkinning()) {
            return SkinningSupport.DISABLED_BY_BATCH;
        }
        return NgSkinningManager.support();
    }

    @Override
    public int submitCount() {
        return submits;
    }

    private boolean batching() {
        return batchDepth > 0;
    }

    private BatchStats stats() {
        // draws / vanillaEndBatches 恒为 0：26.1 的绘制不归本 sink。
        return new BatchStats(submits, skinnedSubmits, seenRenderTypes.size(), 0, 0);
    }

    /**
     * 批次作用域：26.1 没有可合并的自有缓冲，最外层 close 只复位批次状态。
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
                batchDepth = 0;
                policy = BatchPolicy.DEFER;
            }
        }

        @Override
        public BatchStats stats() {
            return DeferredRenderSink.this.stats();
        }

        @Override
        public boolean isOpen() {
            return !closed && batchDepth > 0;
        }
    }

    @Override
    public SubmitNodeCollector submitNodeCollector() {
        return collector;
    }
}
//?}
