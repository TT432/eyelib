package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationSet;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author TT432
 */
public record BrAnimationController(
        String name,
        BrAcState initialState,
        Map<String, BrAcState> states
) implements Animation<BrAnimationController.Data> {
    @Override
    public Data createData() {
        return new Data();
    }

    @Override
    public List<AnimationEffect<?>> getAllEffect() {
        return List.of();
    }

    public static class Data {
        @Setter
        @Getter
        private float startTick;
        @Setter
        @Getter
        private BrAcState lastState;
        @Setter
        @Getter
        private BrAcState currState;
        private final Map<String, List<AnimationEffect.Runtime<?>>> effects = new HashMap<>();
        private final Map<String, Object> data = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <D> D getData(Animation<?> animation) {
            return (D) data.computeIfAbsent(animation.name(), s -> animation.createData());
        }

        public void resetEffects(String animName, AnimationSet targetAnimation) {
            var animation = targetAnimation.animations().get(animName);
            animation.getAllEffect().forEach(effect ->
                    effects.computeIfAbsent(animName, s -> new ArrayList<>()).add(effect.runtime()));
        }

        public void resetEffects(BrAcState currState, AnimationSet targetAnimation) {
            currState.animations().keySet().forEach(animName -> {
                Animation<?> animation = targetAnimation.animations().get(animName);
                animation.getAllEffect().forEach(effect ->
                        effects.computeIfAbsent(animName, s -> new ArrayList<>()).add(effect.runtime()));
            });
        }

        public List<AnimationEffect.Runtime<?>> getEffects(String animName) {
            return effects.get(animName);
        }
    }

    @Override
    public void tickAnimation(Data data, AnimationSet animationSet, MolangScope scope,
                              float ticks, float multiplier, BoneRenderInfos infos,
                              List<AnimationEffect.Runtime<?>> runtime, Runnable loopAction) {
        var currState = data.getCurrState();
        if (currState == null) currState = switchState(ticks, scope, data, animationSet, initialState());

        scope.getOwner().replace(Data.class, data);
        scope.getOwner().replace(AnimationSet.class, animationSet);
        scope.getOwner().replace(BrAcState.class, currState);

        for (Map.Entry<String, MolangValue> entry : currState.transitions().entrySet()) {
            if (entry.getValue().evalAsBool(scope)) {
                currState = switchState(ticks, scope, data, animationSet, states().get(entry.getKey()));
                break;
            }
        }

        scope.getOwner().replace(BrAcState.class, currState);

        blend(animationSet, infos, data, scope, data.getLastState(),
                currState, (ticks - data.getStartTick()) / 20);
    }

    private static BrAcState switchState(float ticks, MolangScope scope, Data data,
                                         AnimationSet animationSet, BrAcState currState) {
        BrAcState lastState = data.getCurrState();

        if (lastState == currState) return currState;

        if (lastState != null) {
            data.setLastState(lastState);
            lastState.onExit().eval(scope);
        }

        currState.onEntry().eval(scope);

        data.setCurrState(currState);
        data.setStartTick(ticks);
        data.resetEffects(currState, animationSet);

        return currState;
    }

    private static void blend(AnimationSet targetAnimations, BoneRenderInfos infos, Data data,
                              MolangScope scope, @Nullable BrAcState lastState, BrAcState currState, float stateTimeSec) {
        float blendProgress;

        if (lastState != null && lastState.blendTransition() != 0) {
            blendProgress = Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        } else {
            blendProgress = 1;
        }

        currState.animations().forEach((animationName, blendValue) -> updateAnimations(targetAnimations, animationName,
                blendProgress * blendValue.eval(scope),
                stateTimeSec, infos, data, scope));

        if (lastState != null && blendProgress < 1) {
            lastState.animations().forEach((animationName, blendValue) -> updateAnimations(targetAnimations, animationName,
                    (1 - blendProgress) * blendValue.eval(scope),
                    stateTimeSec, infos, data, scope));
        }
    }

    private static void updateAnimations(AnimationSet targetAnimations, String animName, float blendValue,
                                         float startedTime, BoneRenderInfos infos, Data data,
                                         MolangScope scope) {
        var animation = targetAnimations.animations().get(animName);

        if (animation == null) return;

        animation.tickAnimation(data.getData(animation), targetAnimations, scope, startedTime,
                Math.clamp(animation.blendWeight().eval(scope), 0, 1) * blendValue, infos,
                data.getEffects(animName), () -> data.resetEffects(animName, targetAnimations));
    }

    record Factory(
            Optional<String> initialState,
            Map<String, BrAcState> states
    ) {
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.optionalFieldOf("initial_state").forGetter(o -> o.initialState),
                Codec.unboundedMap(Codec.STRING, BrAcState.CODEC).fieldOf("states").forGetter(o -> o.states)
        ).apply(ins, Factory::new));

        public static Factory from(BrAnimationController controller) {
            return new Factory(controller.states.entrySet().stream()
                    .filter(e -> e.getValue().equals(controller.initialState))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .or(() -> Optional.of("default")),
                    controller.states);
        }

        public BrAnimationController create(String name) {
            return new BrAnimationController(name, states.get(initialState.orElse("default")), states);
        }
    }
}
