package io.github.tt432.eyelib.client.particle.bedrock.component.particle.initial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @param rotation     degrees
 * @param rotationRate degrees/sec
 * @author TT432
 */
@ParticleComponent(value = "particle_initial_spin", target = ComponentTarget.PARTICLE)
public record ParticleInitialSpin(
        MolangValue rotation,
        MolangValue rotationRate
) {
    public static final Codec<ParticleInitialSpin> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("rotation", MolangValue.FALSE_VALUE).forGetter(o -> o.rotation),
            MolangValue.CODEC.optionalFieldOf("rotation_rate", MolangValue.FALSE_VALUE).forGetter(o -> o.rotationRate)
    ).apply(ins, ParticleInitialSpin::new));
}
