package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibanimation.AnimationComponent;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Bedrock 动画帧调度器，遍历动画组件并驱动每个动画实例前进。
 *
 * @author TT432
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static ModelRuntimeData tickAnimation(AnimationComponent component, MolangScope scope, AnimationEffects effects,
                                                 float ticks, Runnable animationStartFeedback) {
        ModelRuntimeData infos = new ModelRuntimeData();
        var serializableInfo = component.getSerializableInfo();
        if (serializableInfo == null) {
            return infos;
        }

        for (Map.Entry<Animation, MolangValue> entry : component.getAnimate().entrySet()) {
            Animation animation = entry.getKey();
            MolangValue multiplier = entry.getValue();
            if (animation == null) continue;

            animation.tickAnimation(component.getAnimationData(animation.name()),
                    serializableInfo.animations(), scope, ticks, multiplier.eval(scope),
                    infos, effects, animationStartFeedback);
        }

        return infos;
    }
}