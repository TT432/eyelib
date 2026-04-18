package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.particle.ParticleLookup;
import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibmolang.MolangScope;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

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

        scope.getOwner().replace(BrAnimationController.Data.class, data);
        scope.getOwner().replace(BrAnimationController.class, controller);

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

        scope.getOwner().replace(BrAcStateDefinition.class, currState);
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
                    ParticleSpawnService.removeEmitter(particle.particleUUID());
                }
                data.owner().particles().clear();
            }
        }

        currState.onEntry().eval(scope);
        scope.getOwner().onHiveOwners(Entity.class, BrClientEntity.class, (entity, clientEntity) -> {
            for (BrAcParticleEffectDefinition particleEffect : currState.particleEffects()) {
                String uuid = UUID.randomUUID().toString();
                particleEffect.effect().map(clientEntity.particle_effects()::get).ifPresent(effect -> {
                    BrParticle particle = ParticleLookup.get(effect);
                    if (particle != null) {
                        BrParticleEmitter emitter = new BrParticleEmitter(particle, scope, entity.level(), entity.position().toVector3f());
                        ParticleSpawnService.spawnEmitter(uuid, emitter);
                        data.owner().particles().add(new RuntimeParticlePlayData(uuid, emitter, particleEffect.locator().orElse(null), ticks));
                    }
                });
            }
            return Boolean.TRUE;
        });

        data.setCurrState(currState);
        data.setStartTick(ticks);
        currState.animations().keySet().forEach(animName -> {
            String anim = animations.get(animName);
            if (anim == null) return;
            Animation<?> animation = AnimationLookup.get(anim);
            if (animation == null) return;
            animation.onFinishUntyped(data.getData(animation));
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
                    Animation<?> animation = AnimationLookup.get(anim);
                    if (animation == null) return;
                    if (data.getData(animation) instanceof BrAnimationEntry.Data d) {
                        animation.tickAnimationUntyped(data.getData(animation), animations, scope, d.lastTicks(),
                                multiplier * (1 - blendProgress) * blendValue.eval(scope), infos, effects, animationStartFeedback);
                    }
                });
            } else {
                lastState.animations().forEach((name, blendValue) -> {
                    String anim = animations.get(name);
                    if (anim == null) return;
                    Animation<?> animation = AnimationLookup.get(anim);
                    if (animation == null) return;
                    animation.onFinishUntyped(data.getData(animation));
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
        Animation<?> animation = AnimationLookup.get(anim);
        if (animation == null) return;
        animation.tickAnimationUntyped(data.getData(animation), animations, scope, startedTime,
                multiplier * blendValue, infos, effects, animationStartFeedback);
    }
}

