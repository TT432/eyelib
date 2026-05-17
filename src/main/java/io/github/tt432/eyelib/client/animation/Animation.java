package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Map;

/**
 * @author TT432
 */
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
