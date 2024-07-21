package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationSet;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
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

        public List<AnimationEffect.Runtime<?>> getEffects() {
            return effects.values().stream().flatMap(List::stream).toList();
        }
    }

    @Override
    public void tickAnimation(Data data, AnimationSet animationSet, MolangScope scope,
                              float ticks, float multiplier, BoneRenderInfos infos,
                              List<AnimationEffect.Runtime<?>> runtime, Runnable loopAction) {
        var currState = data.getCurrState();
        if (currState == null) currState = switchState(ticks, scope, data, animationSet, initialState());
        currState = currState.transitions().entrySet().stream()
                .filter(e -> e.getValue().evalAsBool(scope))
                .findFirst()
                .map(entry -> switchState(ticks, scope, data, animationSet, states().get(entry.getKey())))
                .orElse(currState);

        float startedTime = (ticks - data.getStartTick()) / 20;

        updateAnimations(animationSet, blend(scope, data.getLastState(), currState, startedTime),
                startedTime, infos, data, scope);
    }

    private static BrAcState switchState(float ticks, MolangScope scope, Data data,
                                         AnimationSet animationSet, BrAcState currState) {
        BrAcState currentState = data.getCurrState();

        if (currentState != null) {
            data.setLastState(currentState);
            currentState.onExit().eval(scope);
        }

        currState.onEntry().eval(scope);

        data.setCurrState(currState);
        data.setStartTick(ticks);
        data.resetEffects(currState, animationSet);

        return currState;
    }

    private static void updateAnimations(AnimationSet targetAnimations, Map<String, Float> blend,
                                         float startedTime, BoneRenderInfos infos, Data data,
                                         MolangScope scope) {
        blend.forEach((animName, blendValue) -> {
            var animation = targetAnimations.animations().get(animName);

            if (animation == null) return;

            animation.tickAnimation(data.getData(animation), targetAnimations, scope, startedTime,
                    animation.blendWeight().eval(scope) * blendValue, infos, data.getEffects(),
                    () -> data.resetEffects(animName, targetAnimations));
        });
    }

    /**
     * @return animation -> scale
     */
    private static Map<String, Float> blend(MolangScope scope, @Nullable BrAcState lastState,
                                            BrAcState currState, float stateTimeSec) {
        float blendProgress;

        if (lastState == null || lastState.blendTransition() == 0) {
            blendProgress = 1;
        } else {
            blendProgress = Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        }

        Map<String, Float> result = new HashMap<>();

        currState.animations().forEach((animationName, blendValue) ->
                result.put(animationName, blendProgress * blendValue.eval(scope)));

        if (lastState != null && blendProgress < 1) {
            lastState.animations().forEach((animationName, blendValue) ->
                    result.put(animationName, result.getOrDefault(animationName, 0F)
                            + (1 - blendProgress) * blendValue.eval(scope)));
        }

        return result;
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
