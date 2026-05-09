package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Root backing adapter for the particle-module {@link ParticleStore} contract.
 * <p>
 * Existing manager ports remain as transitional compatibility accessors while root callers migrate to
 * {@code io.github.tt432.eyelibparticle.api} store/lookup APIs. Remove the compatibility accessors after those
 * callers bind directly to the particle API adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticleManager extends Manager<BrParticle> implements ParticleStore<BrParticle> {
    public static final ParticleManager INSTANCE = new ParticleManager();

    public static ParticleStore<BrParticle> store() {
        return INSTANCE;
    }

    /**
     * Transitional compatibility read accessor for existing root callers.
     */
    public static ManagerReadPort<BrParticle> readPort() {
        return INSTANCE;
    }

    /**
     * Transitional compatibility write accessor for existing root callers.
     */
    public static ManagerWritePort<BrParticle> writePort() {
        return INSTANCE;
    }

    @Override
    public Map<String, BrParticle> all() {
        return getAllData();
    }
}
