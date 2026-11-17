package io.github.tt432.eyelib.client.render;

/**
 * 一帧模型渲染被跳过（没有提交任何几何）的原因。
 * <p>
 * 存在的理由是「静默返回 false」不可诊断：缺失宿主的调用方（伪造宿主的方块锚定渲染、忘记
 * {@code .host(...)} 的瞬发内容）过去只会得到「什么都不显示」，没有日志也没有计数。
 * 所有跳过点必须经 {@link RenderDiagnostics#recordSkip} 记录本枚举，禁止另立字符串/布尔表达。
 *
 * @author TT432
 */
public enum RenderSkipReason {
    /** 未提供宿主（{@link SimpleRenderAction#host()} 为 null）。 */
    MISSING_HOST,
    /** {@code RenderData} 的 molang 作用域未初始化（未 {@code init}/{@code ensureOwner}）。 */
    SCOPE_UNINITIALIZED,
    /** 走 {@code render()} 的路径缺少动画采样（{@code tickedInfos}/{@code effects} 为 null）。 */
    ANIMATION_MISSING,
    /** 没有任何可渲染的模型组件（组件为空，或组件既未就绪也没有可序列化纹理）。 */
    NO_COMPONENTS,
    /** 组件存在但全部未烘焙就绪（{@code readyForRendering()} 为 false）。 */
    NOT_READY,
    /** {@code skipIfUnbaked} 主动跳过本帧并提交后台预热。 */
    NOT_BAKED
}
