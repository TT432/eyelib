package io.github.tt432.eyelibparticle.runtime;

import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared pure-runtime context passed to moved particle runtime objects.
 */
public record ParticleRuntimeContext(
        Optional<MolangScope> parentScope,
        ParticleRuntimeDefinition definition,
        ParticleRuntimeServices services
) {
    public ParticleRuntimeContext {
        parentScope = Objects.requireNonNull(parentScope, "parentScope");
        definition = Objects.requireNonNull(definition, "definition");
        services = Objects.requireNonNull(services, "services");
    }

    public static ParticleRuntimeContext root(ParticleRuntimeDefinition definition, ParticleRuntimeServices services) {
        return new ParticleRuntimeContext(Optional.empty(), definition, services);
    }
}
