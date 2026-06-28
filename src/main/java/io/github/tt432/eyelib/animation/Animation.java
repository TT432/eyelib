package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;

import java.util.Map;
/**
 * 动画运行时接口，定义动画生命周期和数据管理。
 *
 * @author TT432
 */
public interface Animation {
    /**
     * @return name
     * @see AnimationRegistries
     */
    String name();

    Object createData();

    void onFinish(Object data);

    boolean anyAnimationFinished(Object data);

    boolean allAnimationFinished(Object data);

    void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                       ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback);
}
