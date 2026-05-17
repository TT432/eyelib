package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelibanimation.Animation;
import io.github.tt432.eyelibanimation.bedrock.BrAnimation;
import io.github.tt432.eyelibanimation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelibanimation.AnimationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationAssetRegistry {
    private static Map<?, BrAnimation> stagedAnimations = Map.of();
    private static Map<?, BrAnimationControllers> stagedControllers = Map.of();

    public static void stageAnimations(Map<?, BrAnimation> animations) {
        stagedAnimations = animations;
        flushToManager();
    }

    public static void stageControllers(Map<?, BrAnimationControllers> controllers) {
        stagedControllers = controllers;
        flushToManager();
    }

    private static void flushToManager() {
        LinkedHashMap<String, Animation> flattened = new LinkedHashMap<>();
        for (BrAnimation value : stagedAnimations.values()) {
            value.animations().forEach(flattened::put);
        }
        for (BrAnimationControllers value : stagedControllers.values()) {
            value.animationControllers().forEach(flattened::put);
        }
        AnimationManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishAnimation(BrAnimation animation) {
        animation.animations().forEach(AnimationManager.INSTANCE::put);
    }

    public static void publishAnimationController(BrAnimationControllers controller) {
        controller.animationControllers().forEach(AnimationManager.INSTANCE::put);
    }
}
