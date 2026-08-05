package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.particle.api.ParticleSpawnApi;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import org.joml.Vector3f;
import org.joml.Matrix4fc;
/**
 * Root 模块对 AnimationParticleSpawner 的桥接实现。
 * 将 animation 侧的 string 键控 spawn 请求转发到指定的 {@link ParticleSpawnApi} 实例。
 *
 * @author TT432
 */
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
    public void updatePose(String spawnId, Matrix4fc pose) {
        spawner.updatePose(spawnId, pose);
    }

    @Override
    public void remove(String spawnId) {
        spawner.remove(spawnId);
    }

    /**
     * 移除组件登记的全部粒子发射器（实体离场清理）。looping 生命期的发射器不会自然
     * 过期，实体离场后若无显式 remove 将永久驻留 ParticleRenderManager（已实证：实体
     * 全部消失后 99 个 looping 发射器仍存活）。
     */
    public static void removeTracked(io.github.tt432.eyelib.animation.AnimationComponent component,
                                     ParticleSpawnApi spawner) {
        for (var particle : component.drainParticles()) {
            spawner.remove(particle.particleUUID());
        }
    }

    /**
     * flush setup 重建 animationData 时遗弃的粒子登记（每帧调用，空时零开销）。
     */
    public static void flushOrphaned(io.github.tt432.eyelib.animation.AnimationComponent component,
                                     ParticleSpawnApi spawner) {
        for (var particle : component.pollOrphanedParticles()) {
            spawner.remove(particle.particleUUID());
        }
    }
}
