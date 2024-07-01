package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;

/**
 * @author TT432
 */
@ParticleComponent(value = "particle_appearance_lighting", target = ComponentTarget.PARTICLE)
public record ParticleAppearanceLighting() {
    public static final Codec<ParticleAppearanceLighting> CODEC = Codec.unit(new ParticleAppearanceLighting());
}
