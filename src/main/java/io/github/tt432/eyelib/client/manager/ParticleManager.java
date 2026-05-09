package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Root legacy compatibility adapter for callers that still require legacy root particle objects.
 * <p>
 * The active module-owned particle registry is {@code io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry}.
 * Existing manager ports remain as transitional compatibility accessors only while root callers migrate to
 * {@code io.github.tt432.eyelibparticle.api} store/lookup APIs and module runtime definitions. Remove the compatibility
 * accessors after those callers bind directly to the particle API adapters/services.
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
