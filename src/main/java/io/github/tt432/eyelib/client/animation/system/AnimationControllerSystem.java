package io.github.tt432.eyelib.client.animation.system;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.BrBoneAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.component.AnimationControllerComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.MathE;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class AnimationControllerSystem {
    public void update(float ticks) {
        for (var entity : EntityRenderHandler.entities) {
            AnimationControllerComponent component = entity.getAnimationControllerComponent();

            ModelComponent modelComponent = entity.getModelComponent();
            BrModel model = modelComponent.getModel();

            BoneRenderInfos infos = modelComponent.getInfos();
            infos.reset();

            if (model == null || component.getAnimationController() == null) {
                continue;
            }

            for (int i = 0; i < component.getAnimationController().length; i++) {
                BrAnimationController animationController = component.getAnimationController()[i];

                if (animationController == null || component.getTargetAnimation() == null)
                    continue;

                BrAcState currState = component.getCurrState()[i];

                if (currState == null) {
                    switchState(i, ticks, component, animationController.initialState());
                }

                currState = component.getCurrState()[i];

                Map<String, MolangValue> transitions = currState.transitions();

                for (Map.Entry<String, MolangValue> stringMolangValueEntry : transitions.entrySet()) {
                    String stateName = stringMolangValueEntry.getKey();
                    MolangValue predicate = stringMolangValueEntry.getValue();

                    if (predicate.evalAsBool()) {
                        switchState(i, ticks, component, animationController.states().get(stateName));

                        break;
                    }
                }

                float startedTime = (ticks - component.getStartTick()[i]) / 20;
                currState = component.getCurrState()[i];
                Map<String, Float> blend = blend(component.getLastState()[i], currState, startedTime);

                Map<String, BrAnimationEntry> animations = component.getTargetAnimation()[i].animations();

                updateAnimations(animations, blend, startedTime, infos, model.allBones());
            }
        }
    }

    private static void switchState(int idx, float ticks, AnimationControllerComponent component, BrAcState currState) {
        BrAcState lastState = component.getCurrState()[idx];

        if (lastState != null) {
            component.setLastState(idx, lastState);
            lastState.onExit().eval();
        }

        component.setCurrState(idx, currState);
        currState.onEntry().eval();

        component.updateStartTick(idx, ticks);
    }

    private static void updateAnimations(Map<String, BrAnimationEntry> targetAnimations, Map<String, Float> blend,
                                         float startedTime, BoneRenderInfos infos, Map<String, BrBone> stringBrBoneMap) {
        for (Map.Entry<String, Float> animEntry : blend.entrySet()) {
            var animName = animEntry.getKey();
            BrAnimationEntry animation = targetAnimations.get(animName);
            float multiplier = animation.blendWeight().eval() * animEntry.getValue();

            float animTick = startedTime;

            if (startedTime > animation.animationLength() && animation.animationLength() > 0) {
                switch (animation.loop()) {
                    case LOOP -> animTick = startedTime % animation.animationLength();
                    case ONCE -> {
                        continue;
                    }
                    default -> {
                        // no
                    }
                }
            }

            for (Map.Entry<String, BrBoneAnimation> stringBrBoneAnimationEntry : animation.bones().entrySet()) {
                var boneName = stringBrBoneAnimationEntry.getKey();
                var boneAnim = stringBrBoneAnimationEntry.getValue();

                BrBone brBone = stringBrBoneMap.get(boneName);

                if (brBone == null) {
                    continue;
                }

                BoneRenderInfoEntry boneRenderInfoEntry = infos.get(brBone);

                Vector3f p = boneAnim.lerpPosition(animTick);

                if (p != null) {
                    p.div(16).mul(multiplier);

                    boneRenderInfoEntry.getRenderPosition().add(p);
                }

                Vector3f r = boneAnim.lerpRotation(animTick);

                if (r != null) {
                    r.mul(multiplier).mul(EyeMath.DEGREES_TO_RADIANS);

                    boneRenderInfoEntry.getRenderRotation().add(-r.x, -r.y, r.z);
                }

                Vector3f s = boneAnim.lerpScale(animTick);

                if (s != null) {
                    boneRenderInfoEntry.getRenderScala().mul(
                            MathE.notZero(1 + ((s.x - 1) * multiplier), 0.00001F),
                            MathE.notZero(1 + ((s.y - 1) * multiplier), 0.00001F),
                            MathE.notZero(1 + ((s.z - 1) * multiplier), 0.00001F)
                    );
                }

                // TODO other effect
            }
        }
    }

    /**
     * @return animation -> scale
     */
    public Map<String, Float> blend(@Nullable BrAcState lastState, BrAcState currState, float stateTimeSec) {
        float blendProgress = blendProgress(lastState, stateTimeSec);
        Map<String, Float> result = new HashMap<>();

        currState.animations().forEach((animationName, blendValue) ->
                result.put(animationName, blendProgress * blendValue.eval()));

        if (lastState != null && blendProgress < 1) {
            lastState.animations().forEach((animationName, blendValue) ->
                    result.put(animationName, result.getOrDefault(animationName, 0F) + (1 - blendProgress) * blendValue.eval()));
        }

        return result;
    }

    /**
     * @param lastState    current animation controller state
     * @param stateTimeSec second of the time form state start time
     * @return 0 ~ 1
     */
    public float blendProgress(@Nullable BrAcState lastState, float stateTimeSec) {
        return lastState == null ? 1 : MathE.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
    }
}
