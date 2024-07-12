package io.github.tt432.eyelib.client.particle.bedrock.component.particle;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;

/**
 * @author TT432
 */
public interface ParticleParticleComponent extends ParticleComponent {
    default void onStart(BrParticleParticle particle) {
    }

    default void onFrame(BrParticleParticle particle) {
    }
}
