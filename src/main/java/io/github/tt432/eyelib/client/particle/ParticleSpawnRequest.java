package io.github.tt432.eyelib.client.particle;

import org.joml.Vector3f;

import java.util.Objects;

public record ParticleSpawnRequest(String spawnId, String particleId, Vector3f position) {
    public ParticleSpawnRequest {
        spawnId = Objects.requireNonNull(spawnId, "spawnId");
        particleId = Objects.requireNonNull(particleId, "particleId");
        position = new Vector3f(Objects.requireNonNull(position, "position"));
    }
}
