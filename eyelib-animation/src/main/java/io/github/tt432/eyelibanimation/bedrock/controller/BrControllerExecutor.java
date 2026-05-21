package io.github.tt432.eyelibanimation.bedrock.controller;

import io.github.tt432.eyelibanimation.Animation;
import io.github.tt432.eyelibanimation.Animation;
import io.github.tt432.eyelibanimation.AnimationEffects;
import io.github.tt432.eyelibanimation.AnimationLookup;
import io.github.tt432.eyelibanimation.RuntimeParticlePlayData;
import io.github.tt432.eyelibanimation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelibanimation.AnimationParticleSpawner;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateDefinition;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
final class BrControllerExecutor {
    private BrControllerExecutor() {
    }

    static void tick(BrAnimationController controller, BrAnimationController.Data data, Map<String, String> animations, MolangScope scope,
                     float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                     Runnable animationStartFeedback) {
        data.owner().currentAnimations(animations);

        var currState = data.getCurrState();
        if (currState == null) currState = switchState(controller, ticks, scope, data, animations, controller.initialState());
        if (currState == null) return;

        scope.getHostContext().put(BrAnimationController.Data.class, data);
        scope.getHostContext().put(BrAnimationController.class, controller);

        for (Map.Entry<String, io.github.tt432.eyelibmolang.MolangValue> entry : currState.transitions().entrySet()) {
            if (entry.getValue().evalAsBool(scope)) {
                BrAcStateDefinition nextState = controller.states().get(entry.getKey());
                if (nextState == null) break;
                BrAcStateDefinition switchedState = switchState(controller, ticks, scope, data, animations, nextState);
                if (switchedState != null) {
                    currState = switchedState;
                    break;
                }
            }
        }

        scope.getHostContext().put(BrAcStateDefinition.class, currState);
        blend(animations, infos, data, scope, data.getLastState(), currState, multiplier,
                ticks - data.getStartTick(), effects, animationStartFeedback);
        effects.particles.add(data.owner().particles());
    }

    @Nullable
    private static BrAcStateDefinition switchState(BrAnimationController controller, float ticks, MolangScope scope,
                                                   BrAnimationController.Data data, Map<String, String> animations, BrAcStateDefinition currState) {
        BrAcStateDefinition lastState = data.getCurrState();
        if (lastState == currState) return currState;

        if (lastState != null) {
            data.setLastState(lastState);
            lastState.onExit().eval(scope);
            if (!data.owner().particles().isEmpty()) {
                for (var particle : data.owner().particles()) {
                    AnimationParticleSpawner.remove(particle.particleUUID());
                }
                data.owner().particles().clear();
            }
        }

        currState.onEntry().eval(scope);
        scope.getHostContext().get(Entity.class).ifPresent(entity ->
            scope.getHostContext().get(BrClientEntity.class).ifPresent(clientEntity -> {
                for (io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcParticleEffectDefinition particleEffect : currState.particleEffects()) {
                    String uuid = UUID.randomUUID().toString();
                    particleEffect.effect().map(clientEntity.particle_effects()::get).ifPresent(effect -> {
                        ParticleDefinition definition = ParticleDefinitionRegistry.store().get(effect);
                        if (definition != null) {
                            BedrockParticleEmitter emitter = AnimationParticleSpawner.spawn(uuid, definition, entity.position().toVector3f());
                            if (emitter != null) {
                                data.owner().particles().add(new RuntimeParticlePlayData(uuid, emitter, particleEffect.locator().orElse(null), ticks));
                            }
                        }
                    });
                }
            })
        );

        data.setCurrState(currState);
        data.setStartTick(ticks);
        currState.animations().keySet().forEach(animName -> {
            String anim = animations.get(animName);
            if (anim == null) return;
            Animation animation = AnimationLookup.get(anim);
            if (animation == null) return;
            animation.onFinish(data.getData(animation));
        });

        return currState;
    }

    private static void blend(Map<String, String> animations, ModelRuntimeData infos, BrAnimationController.Data data,
                              MolangScope scope, @Nullable BrAcStateDefinition lastState, BrAcStateDefinition currState,
                              float multiplier, float stateTimeSec, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        float blendProgress = lastState != null && lastState.blendTransition() != 0
                ? Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1)
                : 1;

        currState.animations().forEach((animationName, blendValue) ->
                updateAnimations(animations, animationName, blendProgress * blendValue.eval(scope),
                        multiplier, stateTimeSec, infos, data, scope, effects, animationStartFeedback));

        if (lastState != null) {
            if (blendProgress < 1) {
                lastState.animations().forEach((animationName, blendValue) -> {
                    String anim = animations.get(animationName);
                    if (anim == null) return;
                    Animation animation = AnimationLookup.get(anim);
                    if (animation == null) return;
                    if (data.getData(animation) instanceof BrAnimationEntry.Data d) {
                        animation.tickAnimation(data.getData(animation), animations, scope, d.lastTicks(),
                                multiplier * (1 - blendProgress) * blendValue.eval(scope), infos, effects, animationStartFeedback);
                    }
                });
            } else {
                lastState.animations().forEach((name, blendValue) -> {
                    String anim = animations.get(name);
                    if (anim == null) return;
                    Animation animation = AnimationLookup.get(anim);
                    if (animation == null) return;
                    animation.onFinish(data.getData(animation));
                });
            }
        }
    }

    private static void updateAnimations(Map<String, String> animations, String animName, float blendValue,
                                         float multiplier, float startedTime, ModelRuntimeData infos,
                                         BrAnimationController.Data data, MolangScope scope, AnimationEffects effects,
                                         Runnable animationStartFeedback) {
        String anim = animations.get(animName);
        if (anim == null) return;
        Animation animation = AnimationLookup.get(anim);
        if (animation == null) return;
        animation.tickAnimation(data.getData(animation), animations, scope, startedTime,
                multiplier * blendValue, infos, effects, animationStartFeedback);
    }
}
