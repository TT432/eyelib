package io.github.tt432.eyelib.material.port;

import org.jspecify.annotations.Nullable;

/**
 * 绘制档位：决定一段几何在半透明排序里的先后——不透明/cutout 先画，混合（半透明/发光/加法）后画。
 *
 * <p>这是旧 {@code EntityRenderOrchestrator.passOrder} 的 0/1 分档的领域表达，
 * 供组件排序（{@code ModelComponent#passTier()}）与 render sink 的批次关闭顺序共用，
 * 使「不透明先于混合」这一语义只有一处定义。
 *
 * @author TT432
 */
public enum PassTier {
    /** 不透明 / cutout：写深度，不需要按距离排序。 */
    OPAQUE(0),
    /** 混合：必须排在不透明几何之后绘制，且通常需要远到近排序。 */
    BLEND(1);

    private final int order;

    PassTier(int order) {
        this.order = order;
    }

    /** 绘制顺序，小的先画。 */
    public int order() {
        return order;
    }

    /**
     * 由渲染 pass 推导档位。
     *
     * @param pass 端口渲染 pass；{@code null} 表示该组件没有可用的材质 pass
     * @return pass 的档位；{@code null} 按 {@link #OPAQUE} 处理（与旧 passOrder
     *         「无 RenderType 与不透明同档」的语义一致）
     */
    public static PassTier of(@Nullable PortRenderPass pass) {
        return pass == null ? OPAQUE : pass.tier();
    }
}
