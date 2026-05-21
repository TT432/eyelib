package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** @author TT432 */
public record EmitterLocalSpace(
        boolean position,
        boolean rotation,
        boolean velocity
) implements EmitterParticleComponent {
    public static final EmitterLocalSpace EMPTY = new EmitterLocalSpace(false, false, false);

    public static final Codec<EmitterLocalSpace> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("position", false).forGetter(EmitterLocalSpace::position),
            Codec.BOOL.optionalFieldOf("rotation", false).forGetter(EmitterLocalSpace::rotation),
            Codec.BOOL.optionalFieldOf("velocity", false).forGetter(EmitterLocalSpace::velocity)
    ).apply(ins, EmitterLocalSpace::new));
}