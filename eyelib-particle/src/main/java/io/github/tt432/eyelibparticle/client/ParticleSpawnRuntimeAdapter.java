package io.github.tt432.eyelibparticle.client;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Particle-owned spawn/runtime adapter for string-keyed spawn requests.
 */
public final class ParticleSpawnRuntimeAdapter implements ParticleSpawnApi {
    private final ParticleStore<ParticleDefinition> definitions;
    private final ParticleRenderManager renderManager;
    private final Supplier<Optional<ParticleRuntimeEnvironment>> environment;
    private final Supplier<Optional<MolangScope>> parentScope;

    public ParticleSpawnRuntimeAdapter(
            ParticleStore<ParticleDefinition> definitions,
            ParticleRenderManager renderManager,
            Supplier<Optional<ParticleRuntimeEnvironment>> environment,
            Supplier<Optional<MolangScope>> parentScope
    ) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.renderManager = Objects.requireNonNull(renderManager, "renderManager");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.parentScope = Objects.requireNonNull(parentScope, "parentScope");
    }

    @Override
    public void spawn(ParticleSpawnRequest request) {
        Objects.requireNonNull(request, "request");
        ParticleDefinition definition = definitions.get(request.particleId());
        if (definition == null) {
            return;
        }

        Optional<ParticleRuntimeEnvironment> runtimeEnvironment = environment.get();
        if (runtimeEnvironment.isEmpty()) {
            return;
        }

        spawnEmitter(
                request.spawnId(),
                definition,
                parentScope.get(),
                runtimeEnvironment.get(),
                request.position()
        );
    }

    @Override
    public void remove(String spawnId) {
        renderManager.removeEmitter(spawnId);
    }

    public BedrockParticleEmitter spawnEmitter(
            String spawnId,
            ParticleDefinition definition,
            Optional<MolangScope> parentScope,
            ParticleRuntimeEnvironment environment,
            Vector3f position
    ) {
        BedrockParticleEmitter emitter = createEmitter(definition, parentScope, environment, position);
        renderManager.spawnEmitter(spawnId, emitter);
        return emitter;
    }

    public BedrockParticleEmitter createEmitter(
            ParticleDefinition definition,
            Optional<MolangScope> parentScope,
            ParticleRuntimeEnvironment environment,
            Vector3f position
    ) {
        return new BedrockParticleRuntime(
                Objects.requireNonNull(definition, "definition"),
                Objects.requireNonNull(environment, "environment"),
                renderManager::spawnParticle
        ).createEmitter(
                Objects.requireNonNull(parentScope, "parentScope"),
                Objects.requireNonNull(position, "position")
        );
    }
}
