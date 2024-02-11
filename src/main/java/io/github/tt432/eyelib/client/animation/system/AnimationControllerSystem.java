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
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.MathE;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class AnimationControllerSystem {
    @Getter
    private static MolangScope scope;

    public void update(float ticks) {
        for (var entity : EntityRenderHandler.entities) {
            AnimationControllerComponent component = entity.getAnimationControllerComponent();
            scope = entity.getScope();

            ModelComponent modelComponent = entity.getModelComponent();
            BrModel model = modelComponent.getModel();

            BoneRenderInfos infos = modelComponent.getInfos();
            infos.reset();

            if (model == null || component.getAnimationController() == null) {
                continue;
            }

            for (int i = 0; i < component.getAnimationController().size(); i++) {
                component.setCurrentControllerIndex(i);
                BrAnimationController animationController = component.getAnimationController().get(i);

                if (animationController == null || component.getTargetAnimation() == null)
                    continue;

                BrAcState currState = component.getCurrState()[i];

                if (currState == null) {
                    switchState(ticks, component, animationController.initialState());
                }

                currState = component.getCurrState()[i];

                Map<String, MolangValue> transitions = currState.transitions();

                for (Map.Entry<String, MolangValue> stringMolangValueEntry : transitions.entrySet()) {
                    String stateName = stringMolangValueEntry.getKey();
                    MolangValue predicate = stringMolangValueEntry.getValue();

                    if (predicate.evalAsBool(scope)) {
                        switchState(ticks, component, animationController.states().get(stateName));

                        break;
                    }
                }

                float startedTime = (ticks - component.getStartTick()[i]) / 20;
                currState = component.getCurrentState();
                Map<String, Float> blend = blend(component.getLastState()[i], currState, startedTime);

                Map<String, BrAnimationEntry> animations = component.getTargetAnimation().animations();

                updateAnimations(animations, blend, startedTime, infos, model.allBones());
            }
        }
    }

    private static void switchState(float ticks, AnimationControllerComponent component, BrAcState currState) {
        BrAcState lastState = component.getCurrentState();

        if (lastState != null) {
            component.setLastState(lastState);
            lastState.onExit().eval(scope);
        }

        component.setCurrState(currState);
        currState.onEntry().eval(scope);

        component.updateStartTick(ticks);
    }

    private static void updateAnimations(Map<String, BrAnimationEntry> targetAnimations, Map<String, Float> blend,
                                         float startedTime, BoneRenderInfos infos, Map<String, BrBone> stringBrBoneMap) {
        for (Map.Entry<String, Float> animEntry : blend.entrySet()) {
            var animName = animEntry.getKey();
            BrAnimationEntry animation = targetAnimations.get(animName);
            float multiplier = animation.blendWeight().eval(scope) * animEntry.getValue();

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

                Vector3f p = boneAnim.lerpPosition(scope, animTick);

                if (p != null) {
                    p.div(16).mul(multiplier);

                    boneRenderInfoEntry.getRenderPosition().add(p);
                }

                Vector3f r = boneAnim.lerpRotation(scope, animTick);

                if (r != null) {
                    r.mul(multiplier).mul(EyeMath.DEGREES_TO_RADIANS);

                    boneRenderInfoEntry.getRenderRotation().add(-r.x, -r.y, r.z);
                }

                Vector3f s = boneAnim.lerpScale(scope, animTick);

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
                result.put(animationName, blendProgress * blendValue.eval(scope)));

        if (lastState != null && blendProgress < 1) {
            lastState.animations().forEach((animationName, blendValue) ->
                    result.put(animationName, result.getOrDefault(animationName, 0F) + (1 - blendProgress) * blendValue.eval(scope)));
        }

        return result;
    }

    /**
     * @param lastState    current animation controller state
     * @param stateTimeSec second of the time form state start time
     * @return 0 ~ 1
     */
    public float blendProgress(@Nullable BrAcState lastState, float stateTimeSec) {
        if (lastState == null || lastState.blendTransition() == 0) {
            return 1;
        } else {
            return MathE.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        }
    }
}
