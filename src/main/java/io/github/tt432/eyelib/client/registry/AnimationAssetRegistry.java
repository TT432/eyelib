package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationAssetRegistry {
    public static void replaceAssets(Map<?, BrAnimation> animations, Map<?, BrAnimationControllers> controllers) {
        LinkedHashMap<String, Animation<?>> flattened = new LinkedHashMap<>();
        for (BrAnimation value : animations.values()) {
            value.animations().forEach(flattened::put);
        }
        for (BrAnimationControllers value : controllers.values()) {
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
