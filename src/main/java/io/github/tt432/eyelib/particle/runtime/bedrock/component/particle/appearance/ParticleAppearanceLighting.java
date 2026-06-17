package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.appearance;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;

/** @author TT432 */
public record ParticleAppearanceLighting() implements ParticleParticleComponent {
    public static final Codec<ParticleAppearanceLighting> CODEC = Codec.unit(new ParticleAppearanceLighting());
}