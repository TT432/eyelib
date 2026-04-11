package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record BrAnimationControllerSchema(
        String initialState,
        Map<String, BrAcState> states
) {
    public static final Codec<BrAnimationControllerSchema> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("initial_state", "default").forGetter(BrAnimationControllerSchema::initialState),
            Codec.unboundedMap(Codec.STRING, BrAcState.CODEC).fieldOf("states").forGetter(BrAnimationControllerSchema::states)
    ).apply(ins, BrAnimationControllerSchema::new));
}
