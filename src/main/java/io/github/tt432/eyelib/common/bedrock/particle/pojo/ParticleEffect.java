package io.github.tt432.eyelib.common.bedrock.particle.pojo;

import io.github.tt432.eyelib.common.bedrock.particle.pojo.curve.ParticleCurves;
import lombok.Data;

/**
 * @author DustW
 */
@Data
public class ParticleEffect {
    ParticleDescription description;
    ParticleCurves curves;
    ParticleEvents events;
    ParticleComponents components;
}
