package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;

/**
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_appearance_lighting", target = ComponentTarget.PARTICLE)
public record ParticleAppearanceLighting() implements ParticleParticleComponent {
    public static final Codec<ParticleAppearanceLighting> CODEC = Codec.unit(new ParticleAppearanceLighting());
}
