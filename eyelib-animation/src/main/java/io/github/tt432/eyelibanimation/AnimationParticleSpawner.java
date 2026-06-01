package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibparticle.client.ParticleRenderManager;
import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public final class AnimationParticleSpawner {
    private final ParticleSpawnRuntimeAdapter adapter;
    private final ParticleRuntimeEnvironment environment;

    public AnimationParticleSpawner(ParticleRuntimeEnvironment environment) {
        this.environment = environment;
        this.adapter = new ParticleSpawnRuntimeAdapter(
                ParticleDefinitionRegistry.store(),
                ParticleRenderManager.INSTANCE,
                () -> Optional.of(environment),
                Optional::empty
        );
    }

    @Nullable
    public BedrockParticleEmitter spawn(String spawnId, ParticleDefinition definition, Vector3f position) {
        return adapter.spawnEmitter(spawnId, definition, Optional.empty(), environment, position);
    }

    public void remove(String spawnId) {
        ParticleRenderManager.INSTANCE.removeEmitter(spawnId);
    }
}
