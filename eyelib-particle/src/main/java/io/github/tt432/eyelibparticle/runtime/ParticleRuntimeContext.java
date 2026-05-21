package io.github.tt432.eyelibparticle.runtime;

import io.github.tt432.eyelibmolang.MolangScope;

import java.util.Objects;
import java.util.Optional;

/**
 * 传递给迁移后粒子运行时对象的共享纯运行时上下文。
 *
 * @author TT432
 */
/** @author TT432 */
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