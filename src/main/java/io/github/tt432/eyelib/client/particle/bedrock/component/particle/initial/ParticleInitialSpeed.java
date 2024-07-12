package io.github.tt432.eyelib.client.particle.bedrock.component.particle.initial;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_initial_speed", target = ComponentTarget.PARTICLE)
public record ParticleInitialSpeed(
        MolangValue speed
) implements ParticleParticleComponent {
    public static final Codec<ParticleInitialSpeed> CODEC =
            MolangValue.CODEC.xmap(ParticleInitialSpeed::new, ParticleInitialSpeed::speed);

    @Override
    public void onStart(BrParticleParticle particle) {
        particle.setSpeed(speed.eval(particle.molangScope));
    }
}
