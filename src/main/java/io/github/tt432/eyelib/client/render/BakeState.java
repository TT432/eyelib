package io.github.tt432.eyelib.client.render;

/**
 * 烘焙就绪状态（G8）：把「是否已烘焙 / 是否在预热 / 预热是否失败」收敛成三态，
 * 供渲染门控与调用方诊断共用（{@link RenderSkipReason#NOT_BAKED} 是它在跳过原因里的投影）。
 *
 * @author TT432
 */
public enum BakeState {
    /** 已烘焙：本帧可直接绘制。 */
    BAKED,
    /** 未烘焙但预热可进行：本帧绘制会被 {@code skipIfUnbaked} 跳过。 */
    PENDING,
    /** 无法在后台预热（CPU 纹理源缺失）：调用方应放行同步烘焙，否则该内容永不显示。 */
    UNAVAILABLE
}
