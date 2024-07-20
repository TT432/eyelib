package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrAnimator {
    public static BoneRenderInfos tickAnimation(AnimationComponent component, MolangScope scope, float ticks) {
        return component.getTargetAnimation().map(t -> {
            BoneRenderInfos infos = new BoneRenderInfos();
            component.getAnimationController().stream()
                    .filter(Objects::nonNull)
                    .forEach(controller -> {
                        component.setCurrentControllerName(controller.name());
                        tickController(component, controller, ticks, scope, infos);
                    });
            return infos;
        }).orElse(BoneRenderInfos.EMPTY);
    }

    private static void tickController(AnimationComponent component, BrAnimationController animationController,
                                       float ticks, MolangScope scope, BoneRenderInfos infos) {
        var currState = component.getCurrentState()
                .or(() -> Optional.of(switchState(ticks, scope, component, animationController.initialState())))
                .map(state -> state.transitions().entrySet().stream()
                        .filter(e -> e.getValue().evalAsBool(scope))
                        .findFirst()
                        .map(entry -> switchState(ticks, scope, component, animationController.states().get(entry.getKey())))
                        .orElse(state));

        float startedTime = (ticks - component.getStartTick()) / 20;

        component.getTargetAnimation().ifPresent(anim -> currState.ifPresent(cs -> updateAnimations(anim.animations(),
                blend(scope, component.getLastState(), cs, startedTime), startedTime, infos, component, scope)));
    }

    private static BrAcState switchState(float ticks, MolangScope scope, AnimationComponent component, BrAcState currState) {
        component.getCurrentState().ifPresent(lastState -> {
            component.setLastState(lastState);
            lastState.onExit().eval(scope);
        });

        component.setCurrState(currState);
        currState.onEntry().eval(scope);

        component.setStartTick(ticks);

        component.resetSoundEvents(currState);
        component.resetTimelines(currState);

        return currState;
    }

    private static <V> void processEffect(String animaName, float ticks, MolangScope scope,
                                          Map<String, AnimationEffect.Runtime<V>> map) {
        AnimationEffect.Runtime<V> vRuntime = map.get(animaName);

        if (vRuntime != null && !vRuntime.data().isEmpty() && vRuntime.data().firstKey() < ticks) {
            vRuntime.data().pollFirstEntry().getValue().forEach(v -> vRuntime.action().accept(scope, v));
        }
    }

    private static void updateAnimations(Map<String, BrAnimationEntry> targetAnimations, Map<String, Float> blend,
                                         float startedTime, BoneRenderInfos infos, AnimationComponent component,
                                         MolangScope scope) {
        blend.forEach((animName, blendValue) -> {
            BrAnimationEntry animation = targetAnimations.get(animName);

            if (animation == null) return;

            float multiplier = animation.blendWeight().eval(scope) * blendValue;

            evaluateAnimationTime(startedTime, animation, component, animName).ifPresent(animTick -> {
                component.getSoundEffects().ifPresent(sound -> processEffect(animName, animTick, scope, sound));
                component.getTimeline().ifPresent(timeline -> processEffect(animName, animTick, scope, timeline));

                animation.bones().forEach((boneName, boneAnim) -> {
                    BoneRenderInfoEntry entry = infos.get(boneName);

                    boneAnim.lerpPosition(scope, animTick).ifPresent(p ->
                            entry.getRenderPosition().add(p.mul(multiplier / 16)));

                    boneAnim.lerpRotation(scope, animTick).ifPresent(r ->
                            entry.getRenderRotation()
                                    .add(r.mul(multiplier * EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1)));

                    boneAnim.lerpScale(scope, animTick).ifPresent(s -> entry.getRenderScala().mul(
                            EyeMath.notZero(1 + ((s.x - 1) * multiplier), 0.00001F),
                            EyeMath.notZero(1 + ((s.y - 1) * multiplier), 0.00001F),
                            EyeMath.notZero(1 + ((s.z - 1) * multiplier), 0.00001F)
                    ));

                    // TODO other effect
                });
            });
        });
    }

    private static Optional<Float> evaluateAnimationTime(float startedTime, BrAnimationEntry animation,
                                                         AnimationComponent component, String animName) {
        if (startedTime > animation.animationLength() && animation.animationLength() > 0) {
            switch (animation.loop()) {
                case LOOP:
                    component.resetSoundEvent(animName);
                    component.resetTimeline(animName);
                    return Optional.of(startedTime % animation.animationLength());
                case ONCE:
                    return Optional.empty();
            }
        }

        return Optional.of(startedTime);
    }

    /**
     * @return animation -> scale
     */
    private static Map<String, Float> blend(MolangScope scope, Optional<BrAcState> lastState,
                                            BrAcState currState, float stateTimeSec) {
        float blendProgress = lastState.filter(ls -> ls.blendTransition() != 0)
                .map(ls -> Mth.clamp(stateTimeSec / ls.blendTransition(), 0, 1))
                .orElse(1F);

        Map<String, Float> result = currState.animations().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), blendProgress * entry.getValue().eval(scope)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (blendProgress < 1) {
            lastState.ifPresent(ls -> ls.animations().forEach((animationName, blendValue) ->
                    result.put(animationName, result.getOrDefault(animationName, 0F)
                            + (1 - blendProgress) * blendValue.eval(scope))));
        }

        return result;
    }
}
