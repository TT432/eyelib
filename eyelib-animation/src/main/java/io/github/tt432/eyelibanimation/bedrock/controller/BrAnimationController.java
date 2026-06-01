package io.github.tt432.eyelibanimation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibanimation.Animation;
import io.github.tt432.eyelibanimation.AnimationLookup;
import io.github.tt432.eyelibanimation.AnimationEffects;
import io.github.tt432.eyelibanimation.StateMachineAnimation;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
public record BrAnimationController(
        io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerDefinition definition
) implements StateMachineAnimation<BrAcStateDefinition> {
    public BrAnimationController(String name, BrAcState initialState, Map<String, BrAcState> states) {
        this(new io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerDefinition(
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
        return new BrAnimationController(io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerDefinition.fromSchema(name, schema));
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
    public void onFinish(Object data) {
        // TODO: 实现动画完成时的回调逻辑。
    }

    @Override
    public boolean anyAnimationFinished(Object data) {
        if (!(data instanceof Data d) || d.getCurrState() == null) {
            return true;
        }
        return d.getCurrState().animations().keySet().stream().anyMatch(animationName -> {
            String animName = d.owner().currentAnimations().get(animationName);
            if (animName == null) return true;
            Animation animation = AnimationLookup.get(animName);
            if (animation == null) return true;
            return animation.anyAnimationFinished(d.getData(animation));
        });
    }

    @Override
    public boolean allAnimationFinished(Object data) {
        if (!(data instanceof Data d) || d.getCurrState() == null) {
            return false;
        }
        return d.getCurrState().animations().keySet().stream().allMatch(animationName -> {
            String animName = d.owner().currentAnimations().get(animationName);
            if (animName == null) return false;
            Animation animation = AnimationLookup.get(animName);
            if (animation == null) return false;
            return animation.allAnimationFinished(d.getData(animation));
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

        public float animTime() {
            return owner.currentTick() - owner.startTick();
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

        public Object getData(Animation animation) {
            return owner.getData(animation);
        }
    }

    @Override
    public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope,
                              float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        if (data instanceof Data d) {
            BrControllerExecutor.tick(this, d, animations, scope, ticks, multiplier, infos, effects, animationStartFeedback);
        }
    }
}