/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.geckolib3.core.builder;

import io.github.tt432.eyelib.api.animation.LoopType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.github.tt432.eyelib.api.animation.LoopType.EDefaultLoopTypes;

import java.util.List;

/**
 * This class follows the builder pattern, which means that every method returns
 * an instance of this class. You can stack method calls, like this:
 * <code>new AnimationBuilder().addAnimation("jump").addRepeatingAnimation("run", 5");</code>
 */
public class AnimationBuilder {
	private final List<RawAnimation> animationList = new ObjectArrayList<>();

	/**
	 * Add a single animation to the queue and overrides the loop setting
	 *
	 * @param animationName The name of the animation. MUST MATCH THE NAME OF THE
	 *                      ANIMATION IN THE BLOCKBENCH FILE
	 * @param loopType    loop
	 * @return An instance of the current animation builder
	 */
	public AnimationBuilder addAnimation(String animationName, LoopType loopType) {
		animationList.add(new RawAnimation(animationName, loopType));
		return this;
	}

	/**
	 * Add a single animation to the queue
	 *
	 * @param animationName The name of the animation. MUST MATCH THE NAME OF THE
	 *                      ANIMATION IN THE BLOCKBENCH FILE
	 * @return An instance of the current animation builder
	 */
	public AnimationBuilder addAnimation(String animationName) {
		animationList.add(new RawAnimation(animationName, null));
		return this;
	}

	/**
	 * Add multiple animations to the queue and overrides the loop setting to false
	 *
	 * @param animationName The name of the animation. MUST MATCH THE NAME OF THE
	 *                      ANIMATION IN THE BLOCKBENCH FILE
	 * @param timesToRepeat How many times to add the animation to the queue
	 * @return An instance of the current animation builder
	 */
	public AnimationBuilder addRepeatingAnimation(String animationName, int timesToRepeat) {
		if (timesToRepeat < 1) {
			throw new IllegalArgumentException("timesToRepeat must be positive");
		}

		for (int i = 0; i < timesToRepeat; i++) {
			addAnimation(animationName, EDefaultLoopTypes.PLAY_ONCE);
		}

		return this;
	}
	
	public AnimationBuilder playOnce(String animationName) {
		return this.addAnimation(animationName, EDefaultLoopTypes.PLAY_ONCE);
	}
	
	public AnimationBuilder loop(String animationName) {
		return this.addAnimation(animationName, EDefaultLoopTypes.LOOP);
	}
	
	/*
	 * Not implemented yet!
	 */
	public AnimationBuilder playAndHold(String animationName) {
		return this.addAnimation(animationName, EDefaultLoopTypes.HOLD_ON_LAST_FRAME);
	}
	
	//Below will use "Wait instructions", basically empty animations that do nothing, not sure if we really need those honestly
	public AnimationBuilder delayNext(int waitTimeTicks) {
		throw new UnsupportedOperationException("This isn't implemented yet, sorry!");
	}
	
	public AnimationBuilder playAndHoldFor(String animationName, int waitTimeTicks) {
		this.playAndHold(animationName);
		return this.delayNext(waitTimeTicks);
	}

	/**
	 * Clear all the animations in the animation builder.
	 *
	 * @return An instance of the current animation builder
	 */
	public AnimationBuilder clearAnimations() {
		animationList.clear();
		return this;
	}

	/**
	 * Gets the animations currently in this builder.
	 */
	public List<RawAnimation> getRawAnimationList() {
		return animationList;
	}

}
