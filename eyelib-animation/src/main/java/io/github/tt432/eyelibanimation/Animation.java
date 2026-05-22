package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibanimation.AnimationManager;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public interface Animation {
    /**
     * @return name
     * @see AnimationManager
     */
    String name();

    Object createData();

    void onFinish(Object data);

    boolean anyAnimationFinished(Object data);

    boolean allAnimationFinished(Object data);

    void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                       ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback);
}