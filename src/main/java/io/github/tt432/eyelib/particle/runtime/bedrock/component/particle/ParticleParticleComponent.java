package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.joml.Vector3f;

import java.util.Optional;

/** @author TT432 */
public interface ParticleParticleComponent extends ParticleComponent {
    default void onStart(ParticleAccess particle) {
    }

    default void onFrame(ParticleAccess particle) {
    }

    interface ParticleAccess {
        MolangScope molangScope();

        ParticleBlackboard blackboard();

        float lifetime();

        void setLifetime(float lifetime);

        float age();

        void remove();

        void setSpeed(float speed);

        void setRotation(float rotation);

        default float rotation() {
            return 0F;
        }

        void setRotationRate(float rotationRate);

        default float rotationRate() {
            return 0F;
        }

        default Vector3f position() {
            return new Vector3f();
        }

        default Vector3f emitterPosition() {
            return new Vector3f();
        }

        default Vector3f velocity() {
            return new Vector3f();
        }

        default void setVelocity(Vector3f velocity) {
        }

        default Optional<String> blockAtPosition() {
            return Optional.empty();
        }

        /** 世界坐标球域相交的方块碰撞盒（motion_collision 组件用；默认无碰撞）。 */
        default java.util.List<io.github.tt432.eyelib.particle.runtime.bedrock.ParticleRuntimeEnvironment.CollisionBox> collisionBoxes(
                Vector3f worldCenter, float radius) {
            return java.util.List.of();
        }

    }
}