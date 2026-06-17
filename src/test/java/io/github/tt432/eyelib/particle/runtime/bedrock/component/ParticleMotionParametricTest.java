package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionParametric;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class ParticleMotionParametricTest {
    @Test
    void onFrameSetsPositionVelocityAndRotation() {
        ParticleMotionParametric parametric = new ParticleMotionParametric(
                new MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(2), MolangValue.getConstant(3)),
                new MolangValue3(MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO),
                MolangValue.getConstant(30)
        );

        FakeParticle particle = new FakeParticle();
        parametric.onFrame(particle);

        assertEquals(new Vector3f(1, 2, 3), particle.position);
        assertEquals(new Vector3f(0, 1, 0), particle.velocity);
        assertEquals(30F, particle.rotation);
    }

    private static final class FakeParticle implements ParticleParticleComponent.ParticleAccess {
        final MolangScope scope = new MolangScope();
        final ParticleBlackboard blackboard = new ParticleBlackboard();
        float lifetime = 2F;
        float age = 0.5F;
        float speed;
        float rotation;
        float rotationRate;
        Vector3f position = new Vector3f();
        Vector3f velocity = new Vector3f();
        boolean removed;
        String blockId;

        @Override
        public MolangScope molangScope() { return scope; }
        @Override
        public ParticleBlackboard blackboard() { return blackboard; }
        @Override
        public float lifetime() { return lifetime; }
        @Override
        public void setLifetime(float lifetime) { this.lifetime = lifetime; }
        @Override
        public float age() { return age; }
        @Override
        public void remove() { removed = true; }
        @Override
        public void setSpeed(float speed) { this.speed = speed; }
        @Override
        public void setRotation(float rotation) { this.rotation = rotation; }
        @Override
        public void setRotationRate(float rotationRate) { this.rotationRate = rotationRate; }
        @Override
        public float rotation() { return rotation; }
        @Override
        public float rotationRate() { return rotationRate; }
        @Override
        public Vector3f position() { return position; }
        @Override
        public Vector3f velocity() { return velocity; }
        @Override
        public void setVelocity(Vector3f velocity) { this.velocity.set(velocity); }
        @Override
        public Optional<String> blockAtPosition() { return Optional.ofNullable(blockId); }
    }
}
