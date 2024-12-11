package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static BoneRenderInfos tickAnimation(AnimationComponent component, MolangScope scope, float ticks) {
        BoneRenderInfos infos = new BoneRenderInfos();

        component.getAnimate().forEach((animation, multiplier) -> {
            if (animation == null) return;

            animation.tickAnimation(cast(component.getAnimationData(animation.name())),
                    component.getSerializableInfo().animations(), scope, ticks, multiplier.eval(scope), 1, infos);
        });

        return infos;
    }
}
