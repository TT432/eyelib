package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.util.registry.Registry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.LinkedHashMap;
import java.util.Map;


/** @author TT432 */
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
        AnimationRegistries.animation().replaceAll(flattened);
    }

    public static void publishAnimation(BrAnimation animation) {
        Registry<Animation> registry = AnimationRegistries.animation();
        animation.animations().forEach(registry::put);
    }

    public static void publishAnimationController(BrAnimationControllers controller) {
        Registry<Animation> registry = AnimationRegistries.animation();
        controller.animationControllers().forEach(registry::put);
    }
}
