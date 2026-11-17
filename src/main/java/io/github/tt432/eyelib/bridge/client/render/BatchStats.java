package io.github.tt432.eyelib.bridge.client.render;

/**
 * 一批的统计快照（{@link BatchScope#stats()}）。所有计数都是 sink 实例生命周期内的
 * 单调累计值，跨批次不重置（与 {@link RenderSink#submitCount()} 一致）。
 *
 * @param submits         收到的 {@code submit} 次数
 * @param skinnedSubmits  其中成功创建了 GPU 蒙皮会话的次数
 * @param renderTypes     累计出现过的不同 RenderType 实例数（identity 比较，{@code submits} 与它
 *                        的比值即合批率）
 * @param draws           本 sink 自己发起的绘制次数（桶 draw + 蒙皮会话 draw）；26.1 恒为 0
 *                        （绘制归 vanilla {@code renderAllFeatures}）
 * @param vanillaEndBatches 实际调用的 {@code MultiBufferSource.BufferSource.endBatch()} 次数；
 *                        批次内被推迟的 flush 不计入（26.1 恒为 0）
 * @author TT432
 */
public record BatchStats(int submits, int skinnedSubmits, int renderTypes, int draws, int vanillaEndBatches) {
}
