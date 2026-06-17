package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;

/** @author TT432 */
public record ParticleInitialSpin(
        MolangValue rotation,
        MolangValue rotationRate
) implements ParticleParticleComponent {
    public static final Codec<ParticleInitialSpin> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("rotation", MolangValue.FALSE_VALUE).forGetter(ParticleInitialSpin::rotation),
            MolangValue.CODEC.optionalFieldOf("rotation_rate", MolangValue.FALSE_VALUE).forGetter(ParticleInitialSpin::rotationRate)
    ).apply(ins, ParticleInitialSpin::new));

    @Override
    public void onStart(ParticleAccess particle) {
        particle.setRotation(rotation.eval(particle.molangScope()));
        particle.setRotationRate(rotationRate.eval(particle.molangScope()));
    }
}