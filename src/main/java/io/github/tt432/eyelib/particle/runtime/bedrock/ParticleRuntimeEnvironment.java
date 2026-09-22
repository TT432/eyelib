package io.github.tt432.eyelib.particle.runtime.bedrock;

import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Platform-free runtime environment port for Bedrock particle lifecycle code.
 */
/** @author TT432 */
public interface ParticleRuntimeEnvironment {
    int ticks();

    float partialTick();

    default Optional<EmitterParticleComponent.EmitterAccess.Bounds> entityBounds() {
        return Optional.empty();
    }

    default Optional<String> blockAtPosition(Vector3f position) {
        return Optional.empty();
    }

    /**
     * 查询与指定世界坐标球域相交的方块碰撞盒（世界坐标）。
     * 供 {@code minecraft:particle_motion_collision} 组件做碰撞解算；
     * 平台侧按方块实际碰撞形状（VoxelShape）返回，空列表表示无碰撞。
     */
    default java.util.List<CollisionBox> collisionBoxes(Vector3f center, float radius) {
        return java.util.List.of();
    }

    /** 世界坐标轴对齐碰撞盒。 */
    record CollisionBox(double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
    }
}