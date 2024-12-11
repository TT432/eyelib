package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
public record BrAnimationController(
        String name,
        BrAcState initialState,
        Map<String, BrAcState> states
) implements Animation<BrAnimationController.Data> {
    @Override
    public void onFinish(Data data) {
// todo
    }

    @Override
    public boolean isAnimationFinished(Data data) {
        return false; // todo
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
        private BrAcState lastState;
        @Setter
        @Getter
        private BrAcState currState;
        private final Map<String, Object> data = new HashMap<>();
        public final Map<String, String> currentAnimations = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <D> D getData(Animation<?> animation) {
            return (D) data.computeIfAbsent(animation.name(), s -> animation.createData());
        }
    }

    @Override
    public void tickAnimation(Data data, Map<String, String> animations, MolangScope scope,
                              float ticks, float playSpeed, float multiplier, BoneRenderInfos infos) {
        data.currentAnimations.clear();
        data.currentAnimations.putAll(animations);

        var currState = data.getCurrState();
        if (currState == null) currState = switchState(ticks, scope, data, animations, initialState());

        scope.getOwner().replace(Data.class, data);
        scope.getOwner().replace(BrAcState.class, currState);

        for (Map.Entry<String, MolangValue> entry : currState.transitions().entrySet()) {
            if (entry.getValue().evalAsBool(scope)) {
                currState = switchState(ticks, scope, data, animations, states().get(entry.getKey()));
                break;
            }
        }

        scope.getOwner().replace(BrAcState.class, currState);

        blend(animations, infos, data, scope, data.getLastState(), currState, multiplier, playSpeed, ticks - data.getStartTick());
    }

    private static BrAcState switchState(float ticks, MolangScope scope, Data data,
                                         Map<String, String> animations, BrAcState currState) {
        BrAcState lastState = data.getCurrState();

        if (lastState == currState) return currState;

        if (lastState != null) {
            data.setLastState(lastState);
            lastState.onExit().eval(scope);
        }

        currState.onEntry().eval(scope);

        data.setCurrState(currState);
        data.setStartTick(ticks);
        currState.animations().keySet().forEach(animName -> {
            Animation<?> animation = Eyelib.getAnimationManager().get(animations.get(animName));
            if (animation == null) return;
            animation.onFinish(data.getData(animation));
        });

        return currState;
    }

    private static void blend(Map<String, String> animations, BoneRenderInfos infos, Data data,
                              MolangScope scope, @Nullable BrAcState lastState, BrAcState currState,
                              float multiplier, float playSpeed, float stateTimeSec) {
        float blendProgress;

        if (lastState != null && lastState.blendTransition() != 0) {
            blendProgress = Mth.clamp(stateTimeSec / lastState.blendTransition(), 0, 1);
        } else {
            blendProgress = 1;
        }

        currState.animations().forEach((animationName, blendValue) ->
                updateAnimations(animations, animationName, blendProgress * blendValue.eval(scope),
                        multiplier, playSpeed, stateTimeSec, infos, data, scope));

        if (lastState != null && blendProgress < 1) {
            lastState.animations().forEach((animationName, blendValue) ->
                    updateAnimations(animations, animationName, (1 - blendProgress) * blendValue.eval(scope),
                            multiplier, playSpeed, stateTimeSec, infos, data, scope));
        }
    }

    private static void updateAnimations(Map<String, String> animations, String animName, float blendValue,
                                         float multiplier, float playSpeed, float startedTime, BoneRenderInfos infos,
                                         Data data, MolangScope scope) {
        var animation = Eyelib.getAnimationManager().get(animations.get(animName));

        if (animation == null) return;

        animation.tickAnimation(data.getData(animation), animations, scope, startedTime, playSpeed, multiplier * blendValue, infos);
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
