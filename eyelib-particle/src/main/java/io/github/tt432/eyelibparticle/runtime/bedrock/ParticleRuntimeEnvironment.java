package io.github.tt432.eyelibparticle.runtime.bedrock;

import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Platform-free runtime environment port for Bedrock particle lifecycle code.
 */
public interface ParticleRuntimeEnvironment {
    int ticks();

    float partialTick();

    default Optional<EmitterParticleComponent.EmitterAccess.Bounds> entityBounds() {
        return Optional.empty();
    }

    default Optional<String> blockAtPosition(Vector3f position) {
        return Optional.empty();
    }
}
