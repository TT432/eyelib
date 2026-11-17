package io.github.tt432.eyelib.util.event.api;

/**
 * {@link OnRenderStage} 订阅者的调度优先级锚点，供注解参数使用。
 * <p>
 * 必须是 {@code int} 编译期常量：注解属性只能用编译期常量表达式，且 domain 层不得引用
 * Forge / MC 的 {@code EventPriority}。同一优先级内的订阅者按（声明类全名，方法名）排序，
 * 以保证跨 mod 装载顺序、跨运行稳定。
 * <p>
 * 契约边界：优先级只重排<b>同一次 dispatch 内</b>的顺序，不改变「每帧一次、早于 shadow pass」
 * 的整体时机。语义依赖（「必须在 X 之后」）唯一可靠的表达是与 X 取<b>不同档</b>；跨 mod 依赖
 * 请选相距 ≥100 的锚点，避免使用「相邻锚点」这种隐含假设。
 *
 * @author TT432
 */
public final class RenderStagePriority {
    /**
     * 早于组件装配。
     */
    public static final int BEFORE_SETUP = -300;

    /**
     * 早于动画 / RenderContext 采样。改写 {@code yBodyRot} 一类姿态输入用这档。
     */
    public static final int BEFORE_TICK = -100;

    /**
     * eyelib 主管道（默认档，{@link OnRenderStage#priority()} 的默认值）。
     */
    public static final int CORE = 0;

    /**
     * 晚于主管道，但仍可读写主管道已写入的状态。
     */
    public static final int AFTER_TICK = 100;

    /**
     * 晚于主管道全部阶段。
     */
    public static final int AFTER_PIPELINE = 200;

    private RenderStagePriority() {
    }
}
