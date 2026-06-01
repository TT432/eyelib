package io.github.tt432.eyelibparticle.runtime;

import org.joml.Vector3f;

import java.util.Objects;

/**
 * 纯粒子运行时代码的窄运行时服务端口。
 *
 * @author TT432
 */
public record ParticleRuntimeServices(
        TimeSource timeSource,
        ParticleSpawner particleSpawner,
        Environment environment
) {
    public ParticleRuntimeServices {
        Objects.requireNonNull(timeSource, "timeSource");
        Objects.requireNonNull(particleSpawner, "particleSpawner");
        Objects.requireNonNull(environment, "environment");
    }

    public interface TimeSource {
        int ticks();

        float partialTick();
    }

    @FunctionalInterface
    public interface ParticleSpawner {
        void spawn(SpawnRequest request);
    }

    @FunctionalInterface
    public interface Environment {
        String environmentId();
    }

    public record SpawnRequest(String particleId, Vector3f position) {
        public SpawnRequest {
            Objects.requireNonNull(particleId, "particleId");
            position = new Vector3f(Objects.requireNonNull(position, "position"));
        }
    }
}