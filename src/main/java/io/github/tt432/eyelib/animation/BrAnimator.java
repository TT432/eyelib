package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import java.util.Map;

/**
 * Bedrock 动画帧调度器，遍历动画组件并驱动每个动画实例前进。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static ModelRuntimeData tickAnimation(AnimationComponent component, MolangScope scope, AnimationEffects effects,
                                                 float ticks, Runnable animationStartFeedback) {
        return tickAnimation(component, scope, effects, ticks, animationStartFeedback, null);
    }

    public static ModelRuntimeData tickAnimation(AnimationComponent component, MolangScope scope, AnimationEffects effects,
                                                 float ticks, Runnable animationStartFeedback,
                                                 @Nullable Int2ObjectMap<Model.Bone> bindBones) {
        ModelRuntimeData infos = new ModelRuntimeData();
        infos.bindBones(bindBones);
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