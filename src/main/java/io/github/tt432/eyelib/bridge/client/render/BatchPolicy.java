package io.github.tt432.eyelib.bridge.client.render;

/**
 * 批次策略：{@link RenderSink#openBatch(BatchPolicy)} 的最外层策略决定整个批次的
 * 合并行为与蒙皮可用性。
 *
 * @param mergeDraws      sink 是否自建按 RenderType 分桶的几何缓冲（关闭时按 {@link PassTier}
 *                        分组、组内按首次提交顺序各 draw 一次）。false 表示只推迟 flush，
 *                        几何仍走底层 vanilla 通道（可能被 vanilla 按 RenderType 切换打断）。
 * @param allowGpuSkinning 是否允许本次提交走 GPU 蒙皮。false 时 sink 向 writer 传
 *                        {@code skinning=null}：GPU 蒙皮不向 consumer 写顶点，合并模式下桶会空，
 *                        因此必须回到 CPU 顶点写入。
 * @author TT432
 */
public record BatchPolicy(boolean mergeDraws, boolean allowGpuSkinning) {

    /**
     * 只推迟 flush：几何仍进底层 vanilla 通道，蒙皮照常可用。
     * 合批收益来自「一次 flush 覆盖多次 submit」，而非自建分桶。
     */
    public static final BatchPolicy DEFER = new BatchPolicy(false, true);

    /**
     * 自建分桶合并 draw：按 RenderType 收集几何，close 时每组只 draw 一次；
     * 禁用 GPU 蒙皮以保证顶点写入批次缓冲。
     */
    public static final BatchPolicy MERGE = new BatchPolicy(true, false);

    /**
     * 自建分桶合并 draw，但保留 GPU 蒙皮：蒙皮几何不写桶（桶可能为空），
     * 由蒙皮会话在 close 时按 {@link PassTier} 逐会话绘制。
     */
    public static final BatchPolicy MERGE_KEEP_SKINNING = new BatchPolicy(true, true);
}
