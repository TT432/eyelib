package software.bernie.geckolib3.core;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;

public interface IAnimatableModel<E extends IAnimatable> {
	default double getCurrentTick() {
		return System.nanoTime() / 1000000L / 50d;
	}

	default void setCustomAnimations(E animatable, int instanceId) {
		setCustomAnimations(animatable, instanceId, null);
	}

	void setCustomAnimations(E animatable, int instanceId, @Nullable AnimationEvent<E> animationEvent);

	AnimationProcessor<E> getAnimationProcessor();

	Animation getAnimation(String name, IAnimatable animatable);

	/**
	 * Gets a bone by name.
	 *
	 * @param boneName The bone name
	 * @return the bone
	 */
	default IBone getBone(String boneName) {
		IBone bone = getAnimationProcessor().getBone(boneName);

		if (bone == null)
			throw new IllegalArgumentException("Could not find bone: " + boneName);

		return bone;
	}

	void setMolangQueries(IAnimatable animatable, double seekTime);
}
