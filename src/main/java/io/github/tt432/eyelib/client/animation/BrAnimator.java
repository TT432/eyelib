package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static BoneRenderInfos tickAnimation(AnimationComponent component, MolangScope scope, float ticks) {
        BoneRenderInfos infos = new BoneRenderInfos();
        component.getAnimationController().stream()
                .filter(Objects::nonNull)
                .forEach(controller -> {
                    component.setCurrentControllerName(controller.name());
                    controller.tickAnimation(component.currentData(), component.getTargetAnimation(),
                            scope, ticks, 1, infos, List.of(), () -> {
                            });
                });
        return infos;
    }
}
