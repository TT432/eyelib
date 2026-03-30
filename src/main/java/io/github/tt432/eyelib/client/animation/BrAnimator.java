package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static ModelRuntimeData tickAnimation(AnimationComponent component, MolangScope scope, AnimationEffects effects,
                                                 float ticks, Runnable animationStartFeedback) {
        ModelRuntimeData infos = new ModelRuntimeData();
        var serializableInfo = component.getSerializableInfo();
        if (serializableInfo == null) {
            return infos;
        }

        for (Map.Entry<Animation<?>, MolangValue> entry : component.getAnimate().entrySet()) {
            Animation<?> animation = entry.getKey();
            MolangValue multiplier = entry.getValue();
            if (animation == null) continue;

            animation.tickAnimation(cast(component.getAnimationData(animation.name())),
                    serializableInfo.animations(), scope, ticks, multiplier.eval(scope),
                    infos, effects, animationStartFeedback);
        }

        return infos;
    }
}
