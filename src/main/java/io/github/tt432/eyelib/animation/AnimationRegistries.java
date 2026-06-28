package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class AnimationRegistries {
    private static volatile Registry<Animation> animation =
            new Registry<>("AnimationManager", ManagerEventPublisher.NOOP);

    private AnimationRegistries() {
    }

    public static Registry<Animation> animation() {
        return animation;
    }

    public static void register(Registry<Animation> registry) {
        animation = registry;
    }
}
