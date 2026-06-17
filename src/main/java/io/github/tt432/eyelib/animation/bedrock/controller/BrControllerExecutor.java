package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateDefinition;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;

import io.github.tt432.eyelib.util.math.MathHelper;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * @author TT432
 */
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
                AnimationParticleSpawner spawner = scope.getHostContext().get(AnimationParticleSpawner.class).orElse(null);
                if (spawner != null) {
                    for (var particle : data.owner().particles()) {
                        spawner.remove(particle.particleUUID());
                    }
                }
                data.owner().particles().clear();
            }
        }

        currState.onEntry().eval(scope);
        scope.getHostContext().get(Entity.class).ifPresent(entity ->
            scope.getHostContext().get(BrClientEntity.class).ifPresent(clientEntity -> {
                for (io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcParticleEffectDefinition particleEffect : currState.particleEffects()) {
                    String uuid = UUID.randomUUID().toString();
                    particleEffect.effect().map(clientEntity.particle_effects()::get).ifPresent(effect -> {
                        AnimationParticleSpawner spawner = scope.getHostContext().get(AnimationParticleSpawner.class).orElse(null);
                        if (spawner != null) {
                            org.joml.Vector3f position = io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition.resolveLocatorPosition(scope, particleEffect.locator().orElse(null), entity);
                            spawner.spawn(uuid, effect, position);
                            data.owner().particles().add(new RuntimeParticlePlayData(uuid, particleEffect.locator().orElse(null), ticks));
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
                ? MathHelper.clamp(stateTimeSec / lastState.blendTransition(), 0, 1)
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