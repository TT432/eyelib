package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * @author TT432
 */
@Getter
@Nullable
public class AnimationControllerComponent {
    BrAcState[] lastState;
    BrAcState[] currState;
    BrAnimation[] targetAnimation;
    BrAnimationController[] animationController;

    float[] startTick;

    public void setup(BrAnimationController[] animationController, BrAnimation[] targetAnimations) {
        this.animationController = animationController;
        this.targetAnimation = targetAnimations;
        lastState = new BrAcState[animationController.length];
        currState = new BrAcState[animationController.length];
        startTick = new float[animationController.length];
        reset();
    }

    public void reset() {
        Arrays.fill(startTick, -1);
    }

    public void updateStartTick(int idx, float aTick) {
        this.startTick[idx] = aTick;
    }

    public void setLastState(int idx, BrAcState lastState) {
        this.lastState[idx] = lastState;
    }

    public void setCurrState(int idx, BrAcState currState) {
        this.currState[idx] = currState;
    }
}
