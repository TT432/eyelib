package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;

public record EmitterRateInstant(MolangValue numParticles) implements EmitterParticleComponent {
    public static final Codec<EmitterRateInstant> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("num_particles", MolangValue.getConstant(10))
                    .forGetter(EmitterRateInstant::numParticles)
    ).apply(ins, EmitterRateInstant::new));

    @Override
    public void onLoop(EmitterAccess emitter) {
        for (int i = 0; i < (int) numParticles.eval(emitter.molangScope()); i++) {
            emitter.emit();
        }
    }
}
