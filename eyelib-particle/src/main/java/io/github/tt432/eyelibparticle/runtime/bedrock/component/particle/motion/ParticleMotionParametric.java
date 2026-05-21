package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;

/** @author TT432 */
public record ParticleMotionParametric(
        MolangValue3 relativePosition,
        MolangValue3 direction,
        MolangValue rotation
) implements ParticleParticleComponent {
    public static final Codec<ParticleMotionParametric> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("relative_position", MolangValue3.ZERO).forGetter(ParticleMotionParametric::relativePosition),
            MolangValue3.CODEC.optionalFieldOf("direction", MolangValue3.ZERO).forGetter(ParticleMotionParametric::direction),
            MolangValue.CODEC.optionalFieldOf("rotation", MolangValue.ZERO).forGetter(ParticleMotionParametric::rotation)
    ).apply(ins, ParticleMotionParametric::new));

    @Override
    public void onFrame(ParticleAccess particle) {
        particle.position().set(relativePosition.eval(particle.molangScope()));
        particle.setVelocity(direction.eval(particle.molangScope()));
        particle.setRotation(rotation.eval(particle.molangScope()));
    }
}