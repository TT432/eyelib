package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;

/**
 * An AnimationPredicate is run every render frame for ever AnimationController.
 * The "test" method is where you should change animations, stop animations,
 * restart, etc.
 */
@FunctionalInterface
public interface AnimationPredicate<P extends Animatable> {
    /**
     * An AnimationPredicate is run every render frame for ever AnimationController.
     * The "test" method is where you should change animations, stop animations,
     * restart, etc.
     *
     * @return CONTINUE if the animation should continue, STOP if it should stop.
     */
    PlayState test(AnimationEvent<P> event);
}
