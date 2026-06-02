package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleLifetimeExpressionTest {
    @Test
    void onFrameRemovesParticleWhenAgeExceedsMaxLifetime() {
        FakeParticle particle = new FakeParticle();
        particle.age = 1.1F;

        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.getConstant(1)).onStart(particle);
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.getConstant(1)).onFrame(particle);

        assertTrue(particle.removed);
    }

    @Test
    void onFrameDoesNotRemoveParticleWhenMaxLifetimeIsOmitted() {
        FakeParticle particle = new FakeParticle();
        particle.age = 1.1F;

        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE).onStart(particle);
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE).onFrame(particle);

        assertFalse(particle.removed);
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
