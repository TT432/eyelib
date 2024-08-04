package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static BoneRenderInfos tickAnimation(AnimationComponent component, MolangScope scope, float ticks) {
        BoneRenderInfos infos = new BoneRenderInfos();

        for (BrAnimationController controller : component.getAnimationController().values()) {
            if (controller == null) continue;

            component.setCurrentControllerName(controller.name());
            controller.tickAnimation(component.currentData(), component.getTargetAnimation(),
                    scope, ticks, 1, infos, List.of(), () -> {
                    });
        }

        return infos;
    }
}
