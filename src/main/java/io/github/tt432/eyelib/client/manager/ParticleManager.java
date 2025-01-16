package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticleManager extends Manager<BrParticle> {
    public static final ParticleManager INSTANCE = new ParticleManager();
}
