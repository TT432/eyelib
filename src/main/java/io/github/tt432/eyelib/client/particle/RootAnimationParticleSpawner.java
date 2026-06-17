package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.particle.api.ParticleSpawnApi;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

/**
 * Root 模块对 AnimationParticleSpawner 的桥接实现。
 * 将 animation 侧的 string 键控 spawn 请求转发到指定的 {@link ParticleSpawnApi} 实例。
 *
 * @author TT432
 */
@NullMarked
public final class RootAnimationParticleSpawner implements AnimationParticleSpawner {
    private final ParticleSpawnApi spawner;

    public RootAnimationParticleSpawner(ParticleSpawnApi spawner) {
        this.spawner = spawner;
    }

    @Override
    public boolean spawn(String spawnId, String effectId, Vector3f position) {
        spawner.spawn(new ParticleSpawnRequest(spawnId, effectId, position));
        return true;
    }

    @Override
    public void remove(String spawnId) {
        spawner.remove(spawnId);
    }
}
