package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;

public record EmitterRateManual(MolangValue maxParticles) implements EmitterParticleComponent {
    public static final Codec<EmitterRateManual> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("max_particles", MolangValue.getConstant(50))
                    .forGetter(EmitterRateManual::maxParticles)
    ).apply(ins, EmitterRateManual::new));

    @Override
    public boolean canEmit(EmitterAccess emitter) {
        return emitter.emitCount() < maxParticles.eval(emitter.molangScope());
    }
}
