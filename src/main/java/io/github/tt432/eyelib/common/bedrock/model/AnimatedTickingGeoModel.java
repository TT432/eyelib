package io.github.tt432.eyelib.common.bedrock.model;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.Tickable;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.util.molang.MolangParser;

import java.util.Collections;
import java.util.Objects;

public abstract class AnimatedTickingGeoModel<T extends Animatable & Tickable> extends AnimatedGeoModel<T> {
	protected AnimatedTickingGeoModel() {
	}

	public boolean isInitialized() {
		return !this.getAnimationProcessor().getModelRendererList().isEmpty();
	}

	@Override
	public void setCustomAnimations(T animatable, @Nullable Object replaceEntity, int instanceId, @Nullable AnimationEvent<T> animationEvent) {
		// Each animation has its own collection of animations (called the
		// EntityAnimationManager), which allows for multiple independent animations
		AnimationData manager = animatable.getFactory().getOrCreateAnimationData(instanceId);
		if (manager.startTick == -1) {
			manager.startTick = (animatable.tickTimer() + Minecraft.getInstance().getFrameTime());
		}

		if (!Minecraft.getInstance().isPaused() || manager.shouldPlayWhilePaused) {
			manager.tick = (animatable.tickTimer() + Minecraft.getInstance().getFrameTime());
			double gameTick = manager.tick;
			double deltaTicks = gameTick - lastGameTickTime;
			seekTime += deltaTicks;
			lastGameTickTime = gameTick;
		}

		AnimationEvent<T> predicate = Objects.requireNonNullElseGet(animationEvent,
				() -> new AnimationEvent<T>(animatable, 0, 0,
						0, false, Collections.emptyList()));

		predicate.animationTick = seekTime;
		getAnimationProcessor().preAnimationSetup(predicate.getAnimatable(), seekTime, instanceId);
		if (!this.getAnimationProcessor().getModelRendererList().isEmpty()) {
			getAnimationProcessor().tickAnimation(animatable, instanceId, seekTime, predicate,
					MolangParser.getInstance(), shouldCrashOnMissing);
		}

		if (!Minecraft.getInstance().isPaused() || manager.shouldPlayWhilePaused) {
			codeAnimations(animatable, instanceId, animationEvent);
		}
	}

	public void codeAnimations(T entity, Integer uniqueID, AnimationEvent<?> customPredicate) {

	}
}
