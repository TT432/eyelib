package io.github.tt432.eyelibparticle.api;

import org.joml.Vector3f;

import java.util.Objects;

/**
 * String-keyed particle spawn request consumed by runtime adapters.
 *
 * @param spawnId    string identifier for the spawned emitter instance
 * @param particleId string particle definition identifier
 * @param position   spawn position, defensively copied on input and output
 */
public record ParticleSpawnRequest(String spawnId, String particleId, Vector3f position) {
    /**
     * Creates a spawn request with non-null string identifiers and a defensively copied position.
     */
    public ParticleSpawnRequest {
        spawnId = Objects.requireNonNull(spawnId, "spawnId");
        particleId = Objects.requireNonNull(particleId, "particleId");
        position = new Vector3f(Objects.requireNonNull(position, "position"));
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }
}
