package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticleLookupApi;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Transitional root lookup facade delegating active names/definitions to the module definition registry and legacy object
 * lookups to the named compatibility map.
 * <p>
 * {@link ResourceLocation} adaptation intentionally remains at this root boundary. Remove this facade after root callers
 * migrate directly to {@code io.github.tt432.eyelibparticle.api} adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleLookup {
    public static ParticleLookupApi<BrParticle> api() {
        return ParticleManager.store();
    }

    public static @Nullable BrParticle get(ResourceLocation id) {
        return api().get(id.toString());
    }

    public static @Nullable BrParticle get(String id) {
        return api().get(id);
    }

    public static @Nullable ParticleDefinition definition(ResourceLocation id) {
        return definition(id.toString());
    }

    public static @Nullable ParticleDefinition definition(String id) {
        return ParticleDefinitionRegistry.store().get(id);
    }

    public static Collection<String> names() {
        return ParticleDefinitionRegistry.store().names();
    }
}

