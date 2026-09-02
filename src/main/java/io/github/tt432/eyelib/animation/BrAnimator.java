package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
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
        return tickAnimation(component, scope, effects, ticks, animationStartFeedback, bindBones, true);
    }

    /**
     * @param flipAnimation 是否需要按基岩版 geo 约定翻转动画（bbmodel 恒等导入的模型传 false）
     */
    public static ModelRuntimeData tickAnimation(AnimationComponent component, MolangScope scope, AnimationEffects effects,
                                                 float ticks, Runnable animationStartFeedback,
                                                 @Nullable Int2ObjectMap<Model.Bone> bindBones, boolean flipAnimation) {
        ModelRuntimeData infos = new ModelRuntimeData();
        infos.flipAnimation = flipAnimation;
        infos.bindBones(bindBones);
        scope.getHostContext().put(HostRoles.MODEL_RUNTIME_DATA, infos);
        scope.getHostContext().put(HostRoles.ANIMATION_EFFECTS, effects);
        try {
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

            BrAnimationEntryDefinition.updateParticleAnchors(scope, effects);
            effects.commitDeferred();
            return infos;
        } finally {
            scope.getHostContext().remove(HostRoles.ANIMATION_EFFECTS);
            scope.getHostContext().remove(HostRoles.MODEL_RUNTIME_DATA);
        }
    }
}