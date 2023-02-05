package io.github.tt432.eyelib.api.animation;

import io.github.tt432.eyelib.animation.AnimationEntry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import io.github.tt432.eyelib.api.model.Bone;

public interface AnimatableModel<E extends Animatable> {
	default double getCurrentTick() {
		return System.nanoTime() / 1000000L / 50d;
	}

	default void setCustomAnimations(E animatable, int instanceId) {
		setCustomAnimations(animatable, instanceId, null);
	}

	void setCustomAnimations(E animatable, int instanceId, @Nullable AnimationEvent<E> animationEvent);

	AnimationProcessor<E> getAnimationProcessor();

	AnimationEntry getAnimation(String name, Animatable animatable);

	/**
	 * Gets a bone by name.
	 *
	 * @param boneName The bone name
	 * @return the bone
	 */
	default Bone getBone(String boneName) {
		Bone bone = getAnimationProcessor().getBone(boneName);

		if (bone == null)
			throw new IllegalArgumentException("Could not find bone: " + boneName);

		return bone;
	}

	void setMolangQueries(Animatable animatable, double seekTime);
}
