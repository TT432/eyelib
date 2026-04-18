package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Map;

public final class AnimationRuntimes {
    private AnimationRuntimes() {
    }

    @SuppressWarnings("unchecked")
    public static <D> AnimationRuntimePortSet<D> of(Animation<D> animation) {
        if (animation instanceof AnimationRuntimePortSet<?> ports) {
            return (AnimationRuntimePortSet<D>) ports;
        }
        return new LegacyAnimationRuntimeAdapter<>(animation);
    }

    public static String name(Animation<?> animation) {
        return animation.identityPort().name();
    }

    public static Object createState(Animation<?> animation) {
        return createState0(animation);
    }

    public static void finish(Animation<?> animation, Object data) {
        finish0(animation, data);
    }

    public static boolean anyFinished(Animation<?> animation, Object data) {
        return anyFinished0(animation, data);
    }

    public static boolean allFinished(Animation<?> animation, Object data) {
        return allFinished0(animation, data);
    }

    public static void tick(Animation<?> animation, Object data, Map<String, String> animations, MolangScope scope,
                            float ticks, float multiplier, ModelRuntimeData renderInfos, AnimationEffects effects,
                            Runnable animationStartFeedback) {
        tick0(animation, data, animations, scope, ticks, multiplier, renderInfos, effects, animationStartFeedback);
    }

    private static <D> D createState0(Animation<D> animation) {
        return of(animation).state().createData();
    }

    private static <D> void finish0(Animation<D> animation, Object data) {
        var ports = of(animation);
        ports.state().onFinish(ports.state().cast(data));
    }

    private static <D> boolean anyFinished0(Animation<D> animation, Object data) {
        var ports = of(animation);
        return ports.state().anyAnimationFinished(ports.state().cast(data));
    }

    private static <D> boolean allFinished0(Animation<D> animation, Object data) {
        var ports = of(animation);
        return ports.state().allAnimationFinished(ports.state().cast(data));
    }

    private static <D> void tick0(Animation<D> animation, Object data, Map<String, String> animations, MolangScope scope,
                                  float ticks, float multiplier, ModelRuntimeData renderInfos, AnimationEffects effects,
                                  Runnable animationStartFeedback) {
        var ports = of(animation);
        ports.execution().tickAnimation(ports.state().cast(data), animations, scope, ticks, multiplier,
                renderInfos, effects, animationStartFeedback);
    }
}
