package io.github.tt432.eyelib.particle.runtime.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

/**
 * Small factory for module-owned Bedrock particle runtime objects.
 */
/** @author TT432 */
public final class BedrockParticleRuntime {
    private final ParticleDefinition definition;
    private final ParticleRuntimeEnvironment environment;
    private final ParticleRuntimeSpawner spawner;

    public BedrockParticleRuntime(
            ParticleDefinition definition,
            ParticleRuntimeEnvironment environment,
            ParticleRuntimeSpawner spawner
    ) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
    }

    public BedrockParticleEmitter createEmitter(Optional<MolangScope> parentScope, Vector3f position) {
        return new BedrockParticleEmitter(definition, parentScope, environment, spawner, position);
    }
}