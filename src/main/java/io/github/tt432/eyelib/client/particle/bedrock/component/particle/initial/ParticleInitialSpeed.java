package io.github.tt432.eyelib.client.particle.bedrock.component.particle.initial;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author TT432
 */
@ParticleComponent(value = "particle_initial_speed", target = ComponentTarget.PARTICLE)
public record ParticleInitialSpeed(
        MolangValue speed
) {
    public static final Codec<ParticleInitialSpeed> CODEC =
            MolangValue.CODEC.xmap(ParticleInitialSpeed::new, ParticleInitialSpeed::speed);
}
