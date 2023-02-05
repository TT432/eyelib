/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */
package io.github.tt432.eyelib.api.animation;

import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

/**
 * This interface must be applied to any object that wants to be animated
 */
public interface Animatable {
	void registerControllers(AnimationData data);

	AnimationFactory getFactory();
}
