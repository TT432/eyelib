package io.github.tt432.eyelib.client.animation.component;

import com.google.common.collect.ImmutableList;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author TT432
 */
@Nullable
@Getter
public class AnimationComponent {
    BrAcState[] lastState;
    BrAcState[] currState;
    BrAnimation targetAnimation;
    List<BrAnimationController> animationController;

    Map<String, TreeMap<Float, BrEffectsKeyFrame[]>>[] soundEffects;

    float[] startTick;

    @Setter
    int currentControllerIndex;

    public void setup(BrAnimationControllers animationControllers, BrAnimation targetAnimations) {
        this.animationController = ImmutableList.copyOf(animationControllers.animation_controllers().values());
        this.targetAnimation = targetAnimations;
        int animationControllerSize = animationController.size();
        lastState = new BrAcState[animationControllerSize];
        currState = new BrAcState[animationControllerSize];
        startTick = new float[animationControllerSize];
        soundEffects = new Map[animationControllerSize];
        reset();
    }

    public void reset() {
        Arrays.fill(startTick, -1);
    }

    public boolean anyAnimationFinished(float ticks) {
        return getCurrentState().animations().keySet().stream()
                .anyMatch(s -> (ticks - startTick[currentControllerIndex]) > targetAnimation.animations().get(s).animationLength());
    }

    public boolean allAnimationFinished(float ticks) {
        return getCurrentState().animations().keySet().stream()
                .allMatch(s -> (ticks - startTick[currentControllerIndex]) > targetAnimation.animations().get(s).animationLength());
    }

    public BrAcState getCurrentState() {
        return currState[currentControllerIndex];
    }

    public Map<String, TreeMap<Float, BrEffectsKeyFrame[]>> getCurrentSoundEvents() {
        return soundEffects[currentControllerIndex];
    }

    public void updateStartTick(float aTick) {
        this.startTick[currentControllerIndex] = aTick;
    }

    public void setLastState(BrAcState lastState) {
        this.lastState[currentControllerIndex] = lastState;
    }

    public void setCurrState(BrAcState currState) {
        this.currState[currentControllerIndex] = currState;
    }

    public void resetSoundEvents(BrAcState currState) {
        soundEffects[currentControllerIndex] = new HashMap<>();

        currState.animations().forEach((k, v) -> {
            if (targetAnimation.animations().containsKey(k)) {
                soundEffects[currentControllerIndex].put(k, new TreeMap<>(targetAnimation.animations().get(k).soundEffects()));
            }
        });
    }

    public void resetSoundEvent(String animName) {
        BrAnimationEntry brAnimationEntry = targetAnimation.animations().get(animName);

        if (brAnimationEntry != null) {
            soundEffects[currentControllerIndex].put(animName, new TreeMap<>(brAnimationEntry.soundEffects()));
        }
    }
}
