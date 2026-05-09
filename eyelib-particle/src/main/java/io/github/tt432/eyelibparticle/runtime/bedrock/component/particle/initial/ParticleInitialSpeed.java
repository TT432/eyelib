package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;

public record ParticleInitialSpeed(
        MolangValue speed
) implements ParticleParticleComponent {
    public static final Codec<ParticleInitialSpeed> CODEC =
            MolangValue.CODEC.xmap(ParticleInitialSpeed::new, ParticleInitialSpeed::speed);

    @Override
    public void onStart(ParticleAccess particle) {
        particle.setSpeed(speed.eval(particle.molangScope()));
    }
}
