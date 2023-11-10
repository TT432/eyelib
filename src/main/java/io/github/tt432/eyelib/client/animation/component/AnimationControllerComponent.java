package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * @author TT432
 */
@Getter
@Setter
@Nullable
public class AnimationControllerComponent {
    BrAcState lastState;
    BrAcState currState;
    BrAnimation targetAnimation;
    BrAnimationController animationController;

    float startTick = -1;

    public void stop() {
        startTick = -1;
    }

    public void updateStartTick(float aTick) {
        this.startTick = aTick;
    }
}
