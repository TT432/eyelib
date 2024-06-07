package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.BrBoneAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static BoneRenderInfos tickAnimation(AnimationComponent component, MolangScope scope, float ticks) {
        BoneRenderInfos infos = new BoneRenderInfos();

        for (int i = 0; i < component.getAnimationController().size(); i++) {
            component.setCurrentControllerIndex(i);
            BrAnimationController animationController = component.getAnimationController().get(i);

            if (animationController == null || component.getTargetAnimation() == null)
                continue;

            BrAcState currState = component.getCurrState()[i];

            if (currState == null) {
                switchState(ticks, scope, component, animationController.initialState());
            }

            currState = component.getCurrState()[i];

            Map<String, MolangValue> transitions = currState.transitions();

            for (Map.Entry<String, MolangValue> stringMolangValueEntry : transitions.entrySet()) {
                String stateName = stringMolangValueEntry.getKey();
                MolangValue predicate = stringMolangValueEntry.getValue();

                if (predicate.evalAsBool(scope)) {
                    switchState(ticks, scope, component, animationController.states().get(stateName));

                    break;
                }
            }

            float startedTime = (ticks - component.getStartTick()[i]) / 20;
            currState = component.getCurrentState();
            Map<String, Float> blend = blend(scope, component.getLastState()[i], currState, startedTime);

            Map<String, BrAnimationEntry> animations = component.getTargetAnimation().animations();

            updateAnimations(animations, blend, startedTime, infos, component, scope);
        }

        return infos;
    }


    private static void switchState(float ticks, MolangScope scope, AnimationComponent component, BrAcState currState) {
        BrAcState lastState = component.getCurrentState();

        if (lastState != null) {
            component.setLastState(lastState);
            lastState.onExit().eval(scope);
        }

        component.setCurrState(currState);
        currState.onEntry().eval(scope);

        component.updateStartTick(ticks);

        component.resetSoundEvents(currState);
    }

    private static void processSoundEvent(MolangScope scope, float ticks, String animName, AnimationComponent component) {
        Map<String, TreeMap<Float, BrEffectsKeyFrame[]>> currentSoundEvents = component.getCurrentSoundEvents();
        if (currentSoundEvents == null) return;
        TreeMap<Float, BrEffectsKeyFrame[]> soundEffect = currentSoundEvents.get(animName);

        if (soundEffect != null && !soundEffect.isEmpty() && soundEffect.firstKey() < ticks) {
            for (BrEffectsKeyFrame brEffectsKeyFrame : soundEffect.pollFirstEntry().getValue()) {
                Object owner = scope.getOwner().getOwner();

                if (owner instanceof Entity e) {
                    SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(new ResourceLocation(brEffectsKeyFrame.effect()));
                    if (!e.isSilent()) {
                        e.level().playSound(Minecraft.getInstance().player,
                                e.getX(), e.getY(), e.getZ(), soundEvent, e.getSoundSource(), 1, 1);
                    }
                }
            }
        }
    }

    private static void updateAnimations(Map<String, BrAnimationEntry> targetAnimations, Map<String, Float> blend,
                                         float startedTime, BoneRenderInfos infos, AnimationComponent component,
                                         MolangScope scope) {
        for (Map.Entry<String, Float> animEntry : blend.entrySet()) {
            var animName = animEntry.getKey();
            BrAnimationEntry animation = targetAnimations.get(animName);
            float multiplier = animation.blendWeight().eval(scope) * animEntry.getValue();

            float animTick = startedTime;

            if (startedTime > animation.animationLength() && animation.animationLength() > 0) {
                switch (animation.loop()) {
                    case LOOP -> {
                        animTick = startedTime % animation.animationLength();
                        component.resetSoundEvent(animName);
                    }
                    case ONCE -> {
                        continue;
                    }
                    default -> {
                        // no
                    }
                }
            }

            processSoundEvent(scope, animTick, animName, component);

            for (Map.Entry<String, BrBoneAnimation> stringBrBoneAnimationEntry : animation.bones().entrySet()) {
                var boneName = stringBrBoneAnimationEntry.getKey();
                var boneAnim = stringBrBoneAnimationEntry.getValue();
                BoneRenderInfoEntry boneRenderInfoEntry = infos.get(boneName);
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
                            EyeMath.notZero(1 + ((s.x - 1) * multiplier), 0.00001F),
                            EyeMath.notZero(1 + ((s.y - 1) * multiplier), 0.00001F),
                            EyeMath.notZero(1 + ((s.z - 1) * multiplier), 0.00001F)
                    );
                }

                // TODO other effect
            }
        }
    }

    /**
     * @return animation -> scale
     */
    private static Map<String, Float> blend(MolangScope scope, @Nullable BrAcState lastState, BrAcState currState, float stateTimeSec) {
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
    private static float blendProgress(@Nullable BrAcState lastState, float stateTimeSec) {
        if (lastState == null || lastState.blendTransition() == 0) {
            return 1;
        } else {
            return Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        }
    }
}
