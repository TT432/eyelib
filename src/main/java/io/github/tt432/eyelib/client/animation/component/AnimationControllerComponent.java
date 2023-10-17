package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * @author TT432
 */
@Getter
public class AnimationControllerComponent {
    @Nullable
    BrAnimationController animationController;

    float startTick = -1;

    public void stop() {
        startTick = -1;
    }

    public void updateStartTick(float aTick) {
        this.startTick = aTick;
    }
}
