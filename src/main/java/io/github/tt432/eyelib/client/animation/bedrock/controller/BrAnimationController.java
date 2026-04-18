package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.StateMachineAnimation;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
public record BrAnimationController(
        BrAnimationControllerDefinition definition
) implements StateMachineAnimation<BrAnimationController.Data, BrAcStateDefinition> {
    public BrAnimationController(String name, BrAcState initialState, Map<String, BrAcState> states) {
        this(new BrAnimationControllerDefinition(
                name,
                BrAcStateDefinition.fromSchema(initialState),
                states.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BrAcStateDefinition.fromSchema(entry.getValue()),
                        (a, b) -> b,
                        java.util.LinkedHashMap::new
                ))
        ));
    }

    public static final Codec<BrAnimationController> CODEC = Codec.STRING.dispatchStable(
            BrAnimationController::name,
            name -> BrAnimationControllerSchema.CODEC.xmap(
                    schema -> fromSchema(name, schema),
                    BrAnimationController::toSchema
            )
    );

    public static BrAnimationController fromSchema(String name, BrAnimationControllerSchema schema) {
        return new BrAnimationController(BrAnimationControllerDefinition.fromSchema(name, schema));
    }

    public BrAnimationControllerSchema toSchema() {
        return definition.toSchema();
    }

    @Override
    public String name() {
        return definition.name();
    }

    public BrAcStateDefinition initialState() {
        return definition.initialState();
    }

    public Map<String, BrAcStateDefinition> states() {
        return definition.states();
    }

    @Override
    public void onFinish(Data data) {
        // todo
    }

    @Override
    public boolean anyAnimationFinished(Data data) {
        if (data.getCurrState() == null) {
            return true;
        }
        return data.getCurrState().animations().keySet().stream().anyMatch(animationName -> {
            String animName = data.owner().currentAnimations().get(animationName);
            if (animName == null) return true;
            Animation<?> animation = AnimationLookup.get(animName);
            if (animation == null) return true;
            return animation.anyAnimationFinishedUntyped(data.getData(animation));
        });
    }

    @Override
    public boolean allAnimationFinished(Data data) {
        if (data.getCurrState() == null) {
            return false;
        }
        return data.getCurrState().animations().keySet().stream().allMatch(animationName -> {
            String animName = data.owner().currentAnimations().get(animationName);
            if (animName == null) return false;
            Animation<?> animation = AnimationLookup.get(animName);
            if (animation == null) return false;
            return animation.anyAnimationFinishedUntyped(data.getData(animation));
        });
    }

    @Override
    public Data createData() {
        return new Data();
    }

    public static class Data {
        private final BrControllerStateOwner owner = new BrControllerStateOwner();

        BrControllerStateOwner owner() {
            return owner;
        }

        public float getStartTick() {
            return owner.startTick();
        }

        public void setStartTick(float startTick) {
            owner.startTick(startTick);
        }

        @Nullable
        public BrAcStateDefinition getLastState() {
            return owner.lastState();
        }

        public void setLastState(@Nullable BrAcStateDefinition lastState) {
            owner.lastState(lastState);
        }

        @Nullable
        public BrAcStateDefinition getCurrState() {
            return owner.currState();
        }

        public void setCurrState(@Nullable BrAcStateDefinition currState) {
            owner.currState(currState);
        }

        @SuppressWarnings("unchecked")
        public <D> D getData(Animation<?> animation) {
            return owner.getData(animation);
        }
    }

    @Override
    public void tickAnimation(Data data, Map<String, String> animations, MolangScope scope,
                              float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        BrControllerExecutor.tick(this, data, animations, scope, ticks, multiplier, infos, effects, animationStartFeedback);
    }
}

