package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateDefinition;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;

import io.github.tt432.eyelib.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * @author TT432
 */
final class BrControllerExecutor {
    private BrControllerExecutor() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BrControllerExecutor.class);
        private static final java.util.Set<String> WARNED_MISSING_SOUNDS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    static void tick(BrAnimationController controller, BrAnimationController.Data data, Map<String, String> animations, MolangScope scope,
                     float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                     Runnable animationStartFeedback) {
        data.owner().currentAnimations(animations);

        var currState = data.getCurrState();
        if (currState == null) currState = switchState(controller, ticks, scope, data, animations, controller.initialState());
        if (currState == null) return;

        scope.getHostContext().put(HostRoles.CONTROLLER_DATA, data);
        scope.getHostContext().put(HostRoles.ANIMATION_CONTROLLER, controller);
        data.owner().currentTick(ticks);

        for (Map.Entry<String, io.github.tt432.eyelib.molang.MolangValue> entry : currState.transitions().entrySet()) {
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

        scope.getHostContext().put(HostRoles.AC_STATE_DEFINITION, currState);
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
            AnimationParticleSpawner spawner = scope.getHostContext().get(HostRoles.ANIMATION_PARTICLE_SPAWNER).orElse(null);
                if (spawner != null) {
                    for (var particle : data.owner().particles()) {
                        spawner.remove(particle.particleUUID());
                    }
                }
                data.owner().particles().clear();
            }
        }

        currState.onEntry().eval(scope);
        scope.getHostContext().get(HostRoles.CLIENT_ENTITY).ifPresent(clientEntity -> {
            for (io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcParticleEffectDefinition particleEffect : currState.particleEffects()) {
                particleEffect.effect().map(clientEntity.particle_effects()::get).ifPresent(effect ->
                        io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition.requestParticleSpawn(
                                scope,
                                effect,
                                particleEffect.locator().orElse(null),
                                particleEffect.bindToActor(),
                                ticks,
                                data.owner().particles()::add));
            }
        });

        if (!currState.soundEffects().isEmpty()) {
            scope.getHostContext().get(HostRoles.PORT_ENTITY).ifPresent(entity ->
                    scope.getHostContext().get(HostRoles.CLIENT_ENTITY).ifPresent(clientEntity -> {
                        for (String shortName : currState.soundEffects()) {
                            String s = clientEntity.sound_effects().get(shortName);

                            if (s == null) {
                                if (WARNED_MISSING_SOUNDS.add(shortName)) {
                                    LOGGER.warn("Animation controller state sound_effect '{}' has no mapping in sound_effects", shortName);
                                }
                                continue;
                            }

                            io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition.playSound(s, entity);
                        }
                    }));
        }

        data.setCurrState(currState);
        data.setStartTick(ticks);
        currState.animations().keySet().forEach(animName -> {
            Animation animation = data.owner().resolveAnimation(animName);
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
                ? MathHelper.clamp(stateTimeSec / lastState.blendTransition(), 0, 1)
                : 1;

        currState.animations().forEach((animationName, blendValue) ->
                updateAnimations(animations, animationName, blendProgress * blendValue.eval(scope),
                        multiplier, stateTimeSec, infos, data, scope, effects, animationStartFeedback));

        if (lastState != null) {
            if (blendProgress < 1) {
                lastState.animations().forEach((animationName, blendValue) -> {
                    Animation animation = data.owner().resolveAnimation(animationName);
                    if (animation == null) return;
                    if (data.getData(animation) instanceof BrAnimationEntry.Data d) {
                        animation.tickAnimation(data.getData(animation), animations, scope, d.lastTicks(),
                                multiplier * (1 - blendProgress) * blendValue.eval(scope), infos, effects, animationStartFeedback);
                    }
                });
            } else {
                lastState.animations().forEach((name, blendValue) -> {
                    Animation animation = data.owner().resolveAnimation(name);
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
        Animation animation = data.owner().resolveAnimation(animName);
        if (animation == null) return;
        animation.tickAnimation(data.getData(animation), animations, scope, startedTime,
                multiplier * blendValue, infos, effects, animationStartFeedback);
    }
}