package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
public record BrAnimationController(
        String name,
        BrAcState initialState,
        Map<String, BrAcState> states
) {
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
