package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;

/**
 * 一次动画 tick 的产物（G8）。
 * <p>
 * {@code tickedInfos} 可直接交给 {@link SimpleRenderAction.Builder#animation(ModelRuntimeData, AnimationEffects)}
 * 或忽略：同一次 tick 已把采样写回动画组件，因此 {@code .animation(animationComponent)} 拿到同一份结果。
 *
 * @param tickedInfos 本帧动画采样；无动画数据时为 {@link ModelRuntimeData#EMPTY}
 * @param effects     本帧效果收集器（已提交的动画效果）
 * @param ticked      {@code false} 表示前置条件不满足（scope 未初始化），本帧未执行任何 tick
 * @author TT432
 */
public record RenderTickResult(ModelRuntimeData tickedInfos, AnimationEffects effects, boolean ticked) {

    /** 前置条件不满足：未执行 tick（两个字段为安全默认值，调用方不得当作「无动画」）。 */
    public static final RenderTickResult NOT_INITIALIZED =
            new RenderTickResult(ModelRuntimeData.EMPTY, new AnimationEffects(), false);
}
