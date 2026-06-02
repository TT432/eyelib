package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelibanimation.AnimationParticleSpawner;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

/**
 * Root 模块对 AnimationParticleSpawner 的桥接实现。
 * 将 animation 侧的 string 键控 spawn 请求转发到 ParticleSpawnService。
 *
 * @author TT432
 */
@NullMarked
public final class RootAnimationParticleSpawner implements AnimationParticleSpawner {
    @Override
    public boolean spawn(String spawnId, String effectId, Vector3f position) {
        ParticleSpawnService.api().spawn(new ParticleSpawnRequest(spawnId, effectId, position));
        return true;
    }

    @Override
    public void remove(String spawnId) {
        ParticleSpawnService.api().remove(spawnId);
    }
}
