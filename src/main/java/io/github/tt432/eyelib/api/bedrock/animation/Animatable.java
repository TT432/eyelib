/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */
package io.github.tt432.eyelib.api.bedrock.animation;

import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationFactory;

/**
 * This interface must be applied to any object that wants to be animated
 */
public interface Animatable {
    void registerControllers(AnimationData data);

    AnimationFactory getFactory();
}
