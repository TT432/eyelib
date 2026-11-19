package io.github.tt432.eyelib.bridge.client.render;

/**
 * {@link RenderSink} 的绘制粒度（诊断/决策用，不改变 sink 自身行为）。
 *
 * @author TT432
 */
public enum FlushMode {
    /**
     * 每次 {@code submit} 独立决定绘制：顶点被交给一个独立的提交单元（>=26.1 的
     * {@code SubmitNodeCollector} 节点），跨 submit 无法在本 sink 内合并。
     */
    PER_SUBMIT,
    /**
     * 绘制以 flush / 批次为单位：顶点写入缓冲（vanilla MultiBufferSource 或自建桶），
     * 只有 flush 或批次 close 时才 {@code endBatch} / draw。
     */
    PER_BATCH
}
