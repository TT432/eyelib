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

    default AnimationRuntimePortSet<D> ports() {
        return AnimationRuntimes.of(this);
    }

    default AnimationIdentityPort identityPort() {
        return ports().identity();
    }

    default AnimationStatePort<D> statePort() {
        return ports().state();
    }

    default AnimationExecutionPort<D> executionPort() {
        return ports().execution();
    }

    default Object createDataUntyped() {
        return statePort().createData();
    }

    default void onFinishUntyped(Object data) {
        statePort().onFinish(statePort().cast(data));
    }

    default boolean anyAnimationFinishedUntyped(Object data) {
        return statePort().anyAnimationFinished(statePort().cast(data));
    }

    default boolean allAnimationFinishedUntyped(Object data) {
        return statePort().allAnimationFinished(statePort().cast(data));
    }

    default void tickAnimationUntyped(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                      ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
        executionPort().tickAnimation(statePort().cast(data), animations, scope, ticks, multiplier,
                renderInfos, effects, animationStartFeedback);
    }

    void onFinish(D data);

    boolean anyAnimationFinished(D data);

    boolean allAnimationFinished(D data);

    D createData();

    void tickAnimation(D data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                       ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback);
}
