package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeKillPlane;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleLifetimeKillPlaneTest {
    @Test
    void onFrameDoesNotRemoveParticleAboveKillPlane() {
        ParticleLifetimeKillPlane killPlane = new ParticleLifetimeKillPlane(new Vector4f(0, 1, 0, 0));

        FakeParticle above = new FakeParticle();
        above.position.set(0, 1, 0);
        killPlane.onFrame(above);

        assertFalse(above.removed);
    }

    @Test
    void onFrameRemovesParticleThatCrossesKillPlane() {
        ParticleLifetimeKillPlane killPlane = new ParticleLifetimeKillPlane(new Vector4f(0, 1, 0, 0));

        FakeParticle crossed = new FakeParticle();
        crossed.position.set(0, -1, 0);
        killPlane.onFrame(crossed);

        assertTrue(crossed.removed);
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
