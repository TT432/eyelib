package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Map;

/**
 * @author TT432
 */
public interface Animation<D> {
    /**
     * @return name
     * @see AnimationManager
     */
    String name();

    default Object createDataUntyped() {
        return createData();
    }

    @SuppressWarnings("unchecked")
    default void onFinishUntyped(Object data) {
        onFinish((D) data);
    }

    @SuppressWarnings("unchecked")
    default boolean anyAnimationFinishedUntyped(Object data) {
        return anyAnimationFinished((D) data);
    }

    @SuppressWarnings("unchecked")
    default boolean allAnimationFinishedUntyped(Object data) {
        return allAnimationFinished((D) data);
    }

    @SuppressWarnings("unchecked")
    default void tickAnimationUntyped(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                      ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
        tickAnimation((D) data, animations, scope, ticks, multiplier,
                renderInfos, effects, animationStartFeedback);
    }

    void onFinish(D data);

    boolean anyAnimationFinished(D data);

    boolean allAnimationFinished(D data);

    D createData();

    void tickAnimation(D data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                       ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback);
}
