package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import lombok.Getter;

/**
 * @author TT432
 */
@Getter
public class AnimationComponent {
    final BrAnimationEntry currentAnimation;
    final BrAnimationController animationController;

    float startTick = -1;

    public AnimationComponent(BrAnimationEntry currentAnimation, BrAnimationController animationController) {
        this.currentAnimation = currentAnimation;
        this.animationController = animationController;
    }

    public void stop() {
        startTick = -1;
    }

    public void updateStartTick(float aTick) {
        this.startTick = aTick;
    }
}
