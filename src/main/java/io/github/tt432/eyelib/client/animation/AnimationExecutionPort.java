package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Map;

public interface AnimationExecutionPort<D> {
    void tickAnimation(D data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                       ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback);
}
