package io.github.tt432.eyelib.client.animation.system;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public class AnimationSystem {
    public void update(float aTick) {
        for (var entity : EntityRenderHandler.entities) {
            if (entity == null) continue;

            AnimationComponent animationComponent = entity.getAnimationComponent();
            BrAnimationEntry currentAnimation = animationComponent.getCurrentAnimation();

            if (currentAnimation == null) {
                continue;
            }

            float startTick = animationComponent.getStartTick();

            if (startTick == -1) {
                animationComponent.updateStartTick(aTick);
            }

            float animTick = (aTick - startTick) / 20F;

            if (animTick > currentAnimation.animationLength()) {
                animationComponent.updateStartTick(aTick);
            }

            ModelComponent modelComponent = entity.getModelComponent();

            updateBoneAnimation(currentAnimation, animTick, modelComponent);

            // TODO other animation data
        }
    }

    private static void updateBoneAnimation(BrAnimationEntry currentAnimation, float animTick, ModelComponent modelComponent) {
        var model = modelComponent.getModel();

        if (model == null)
            return;

        Map<String, BrBone> allBones = model.allBones();

        // TODO
        // for (var boneAnimation : currentAnimation.bones().entrySet()) {
        //     BrBoneAnimation value = boneAnimation.getValue();
        //     BrBone bone = allBones.get(boneAnimation.getKey());

        //     if (bone == null)
        //         continue;

        //     Vector3f p = value.lerpPosition(animTick);

        //     if (p != null) {
        //         bone.setRenderPivot(new Vector3f(bone.pivot()).add(p));
        //     } else {
        //         bone.setRenderPivot(null);
        //     }

        //     Vector3f r = value.lerpRotation(animTick);

        //     if (r != null) {
        //         bone.setRenderRotation(new Vector3f(bone.rotation())
        //                 .add((float) -Math.toRadians(r.x),
        //                         (float) -Math.toRadians(r.y),
        //                         (float) Math.toRadians(r.z)));
        //     } else {
        //         bone.setRenderRotation(null);
        //     }

        //     Vector3f s = value.lerpScale(animTick);

        //     if (s != null) {
        //         bone.setRenderScala(new Vector3f(MathE.notZero(s.x, 0.00001F),
        //                 MathE.notZero(s.y, 0.00001F),
        //                 MathE.notZero(s.z, 0.00001F)));
        //     } else {
        //         bone.setRenderScala(new Vector3f(1));
        //     }
        // }
    }
}
