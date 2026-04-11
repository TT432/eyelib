package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcParticleEffect;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;


import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelib.client.particle.ParticleLookup;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author TT432
 */
public record BrAnimationController(
        String name,
        BrAcState initialState,
        Map<String, BrAcState> states
) implements Animation<BrAnimationController.Data> {
    // Helper to return null from lambdas with proper NullAway suppression
    @SuppressWarnings("NullAway")
    @Nullable
    private static <T> T nil() {
        @SuppressWarnings("NullAway")
        T result = null;
        return result;
    }

    public static final Codec<BrAnimationController> CODEC = Codec.STRING.dispatchStable(
            BrAnimationController::name,
            name -> BrAnimationControllerSchema.CODEC.xmap(
                    schema -> fromSchema(name, schema),
                    BrAnimationController::toSchema
            )
    );

    public static BrAnimationController fromSchema(String name, BrAnimationControllerSchema schema) {
        BrAcState initial = schema.states().get(schema.initialState());
        if (initial == null) {
            initial = schema.states().get("default");
        }
        if (initial == null) {
            initial = new BrAcState(Map.of(), MolangValue.ZERO, MolangValue.ZERO, List.of(), List.of(), Map.of(), 0F, false);
        }
        return new BrAnimationController(name, initial, schema.states());
    }

    public BrAnimationControllerSchema toSchema() {
        for (Map.Entry<String, BrAcState> entry : states.entrySet()) {
            if (entry.getValue().equals(initialState)) {
                return new BrAnimationControllerSchema(entry.getKey(), states);
            }
        }
        return new BrAnimationControllerSchema("default", states);
    }

    @Override
    public void onFinish(Data data) {
// todo
    }

    @Override
    public boolean anyAnimationFinished(Data data) {
        if (data.currState == null) {
            return true;
        }
        return data.currState.animations().keySet().stream().anyMatch(animationName -> {
            String animName = data.currentAnimations.get(animationName);
            if (animName == null) return true;
            Animation<?> animation = AnimationLookup.get(animName);
            if (animation == null) return true;
            return animation.anyAnimationFinished(data.getData(animation));
        });
    }

    @Override
    public boolean allAnimationFinished(Data data) {
        if (data.currState == null) {
            return false;
        }
        return data.currState.animations().keySet().stream().allMatch(animationName -> {
            String animName = data.currentAnimations.get(animationName);
            if (animName == null) return false;
            Animation<?> animation = AnimationLookup.get(animName);
            if (animation == null) return false;
            return animation.anyAnimationFinished(data.getData(animation));
        });
    }

    @Override
    public Data createData() {
        return new Data();
    }

    public static class Data {
        @Setter
        @Getter
        private float startTick = -1;
        @Setter
        @Getter
        @Nullable
        private BrAcState lastState;
        @Setter
        @Getter
        @Nullable
        private BrAcState currState;
        private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
        public Map<String, String> currentAnimations = new Object2ObjectOpenHashMap<>();
        private final List<RuntimeParticlePlayData> particles = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public <D> D getData(Animation<?> animation) {
            return (D) data.computeIfAbsent(animation.name(), s -> animation.createData());
        }
    }

    @Override
    public void tickAnimation(Data data, Map<String, String> animations, MolangScope scope,
                              float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        data.currentAnimations = animations;

        var currState = data.getCurrState();
        if (currState == null) currState = switchState(ticks, scope, data, animations, initialState());
        if (currState == null) {
            return;
        }

        scope.getOwner().replace(Data.class, data);
        scope.getOwner().replace(BrAnimationController.class, this);

        for (Map.Entry<String, MolangValue> entry : currState.transitions().entrySet()) {
            if (entry.getValue().evalAsBool(scope)) {
                BrAcState nextState = states().get(entry.getKey());
                if (nextState == null) break;
                BrAcState switchedState = switchState(ticks, scope, data, animations, nextState);
                if (switchedState != null) {
                    currState = switchedState;
                    break;
                }
            }
        }

        scope.getOwner().replace(BrAcState.class, currState);

        blend(animations, infos, data, scope, data.getLastState(), currState, multiplier,
                ticks - data.getStartTick(), effects, animationStartFeedback);

        effects.particles.add(data.particles);
    }

    @Nullable
    private static BrAcState switchState(float ticks, MolangScope scope, Data data,
                                         Map<String, String> animations, BrAcState currState) {
        BrAcState lastState = data.getCurrState();

        if (lastState == currState) return currState;

        if (lastState != null) {
            data.setLastState(lastState);
            lastState.onExit().eval(scope);
            if (!data.particles.isEmpty()) {
                for (var particle : data.particles) {
                    ParticleSpawnService.removeEmitter(particle.particleUUID());
                }

                data.particles.clear();
            }
        }

        currState.onEntry().eval(scope);
        scope.getOwner().onHiveOwners(Entity.class, BrClientEntity.class, (entity, clientEntity) -> {
            for (BrAcParticleEffect particleEffect : currState.particleEffects()) {
                String uuid = UUID.randomUUID().toString();
                particleEffect.effect().map(clientEntity.particle_effects()::get).ifPresent(effect -> {
                    BrParticle particle = ParticleLookup.get(effect);
                    if (particle != null) {
                        BrParticleEmitter emitter = new BrParticleEmitter(particle, scope, entity.level(), entity.position().toVector3f());
                        ParticleSpawnService.spawnEmitter(uuid, emitter);
                        data.particles.add(new RuntimeParticlePlayData(uuid, emitter, particleEffect.locator().orElse(null), ticks));
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
            animation.onFinish(data.getData(animation));
        });

        return currState;
    }

    private static void blend(Map<String, String> animations, ModelRuntimeData infos, Data data,
                              MolangScope scope, @Nullable BrAcState lastState, BrAcState currState,
                              float multiplier, float stateTimeSec, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        float blendProgress;

        if (lastState != null && lastState.blendTransition() != 0) {
            blendProgress = Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        } else {
            blendProgress = 1;
        }

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
                        animation.tickAnimation(data.getData(animation), animations, scope, d.lastTicks,
                                multiplier * (1 - blendProgress) * blendValue.eval(scope), infos, effects, animationStartFeedback);
                    }
                });
            } else {
                lastState.animations().forEach((name, blendValue) -> {
                    String anim = animations.get(name);
                    if (anim == null) return;
                    Animation<?> animation = AnimationLookup.get(anim);
                    if (animation == null) return;

                    animation.onFinish(data.getData(animation));
                });
            }
        }
    }

    private static void updateAnimations(Map<String, String> animations, String animName, float blendValue,
                                         float multiplier, float startedTime, ModelRuntimeData infos,
                                         Data data, MolangScope scope, AnimationEffects effects,
                                         Runnable animationStartFeedback) {
        String anim = animations.get(animName);
        if (anim == null) return;
        Animation<?> animation = AnimationLookup.get(anim);
        if (animation == null) return;

        animation.tickAnimation(data.getData(animation), animations, scope, startedTime,
                multiplier * blendValue, infos, effects, animationStartFeedback);
    }

}
