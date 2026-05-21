package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue2;
import io.github.tt432.eyelibmolang.MolangValue4;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceBillboard;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceLighting;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceTinting;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial.ParticleInitialSpeed;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.initial.ParticleInitialSpin;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleExpireIfInBlocks;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleExpireIfNotInBlocks;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeKillPlane;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionDynamic;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion.ParticleMotionParametric;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleComponentRuntimeTest {
    @Test
    void billboardLightingTintingAndInitialComponentsPreserveParticleBehavior() {
        FakeParticle particle = new FakeParticle();

        ParticleAppearanceBillboard billboard = new ParticleAppearanceBillboard(
                new MolangValue2(MolangValue.getConstant(2), MolangValue.getConstant(4)),
                ParticleAppearanceBillboard.FaceCameraMode.EMITTER_TRANSFORM_XY,
                ParticleAppearanceBillboard.Direction.EMPTY,
                new ParticleAppearanceBillboard.UV(
                        16,
                        16,
                        new MolangValue2(MolangValue.getConstant(4), MolangValue.getConstant(8)),
                        new MolangValue2(MolangValue.getConstant(2), MolangValue.getConstant(4)),
                        ParticleAppearanceBillboard.UV.Flipbook.EMPTY
                )
        );

        assertEquals(new Vector2f(2, 4), billboard.getSize(particle));
        assertEquals(new Vector4f(0.25F, 0.5F, 0.125F, 0.25F), billboard.getUV(particle));
        assertInstanceOf(ParticleParticleComponent.class, new ParticleAppearanceLighting());

        ParticleAppearanceTinting staticTint = new ParticleAppearanceTinting(
                false,
                new MolangValue4(MolangValue.ONE, MolangValue.getConstant(0.5F), MolangValue.ZERO, MolangValue.ONE),
                null
        );
        assertEquals(0xFFFF7F00, staticTint.getColor(particle));

        TreeMap<Float, Integer> gradient = new TreeMap<>();
        gradient.put(0F, 0xFF000000);
        gradient.put(1F, 0xFFFFFFFF);
        ParticleAppearanceTinting gradientTint = new ParticleAppearanceTinting(
                true,
                null,
                new ParticleAppearanceTinting.Color(gradient, MolangValue.getConstant(0.5F))
        );
        assertEquals(0xFF7F7F7F, gradientTint.getColor(particle));

        new ParticleInitialSpeed(MolangValue.getConstant(3)).onStart(particle);
        new ParticleInitialSpin(MolangValue.getConstant(45), MolangValue.getConstant(90)).onStart(particle);

        assertEquals(3F, particle.speed);
        assertEquals(45F, particle.rotation);
        assertEquals(90F, particle.rotationRate);
    }

    @Test
    void lifetimeAndMotionComponentsPreserveParticleFrameBehavior() {
        FakeParticle lifetimeParticle = new FakeParticle();
        lifetimeParticle.age = 1.1F;
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.getConstant(1)).onStart(lifetimeParticle);
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.getConstant(1)).onFrame(lifetimeParticle);
        assertTrue(lifetimeParticle.removed);

        FakeParticle omittedMaxLifetimeParticle = new FakeParticle();
        omittedMaxLifetimeParticle.age = 1.1F;
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE).onStart(omittedMaxLifetimeParticle);
        new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE).onFrame(omittedMaxLifetimeParticle);
        assertFalse(omittedMaxLifetimeParticle.removed);

        ParticleLifetimeKillPlane killPlane = new ParticleLifetimeKillPlane(new Vector4f(0, 1, 0, 0));
        FakeParticle aboveKillPlaneParticle = new FakeParticle();
        aboveKillPlaneParticle.position.set(0, 1, 0);
        killPlane.onFrame(aboveKillPlaneParticle);
        assertFalse(aboveKillPlaneParticle.removed);

        FakeParticle crossedKillPlaneParticle = new FakeParticle();
        crossedKillPlaneParticle.position.set(0, -1, 0);
        killPlane.onFrame(crossedKillPlaneParticle);
        assertTrue(crossedKillPlaneParticle.removed);

        FakeParticle dynamicParticle = new FakeParticle();
        dynamicParticle.age = 0.5F;
        dynamicParticle.velocity = new Vector3f(1, 0, 0);
        new ParticleMotionDynamic(
                new io.github.tt432.eyelibmolang.MolangValue3(MolangValue.getConstant(1), MolangValue.ZERO, MolangValue.ZERO),
                MolangValue.ZERO,
                MolangValue.getConstant(10),
                MolangValue.ZERO
        ).onFrame(dynamicParticle);
        assertEquals(new Vector3f(0.046875F, 0F, 0F), dynamicParticle.position);
        assertEquals(5F, dynamicParticle.rotationRate);
        assertEquals(2.5F, dynamicParticle.rotation);

        FakeParticle blockParticle = new FakeParticle();
        blockParticle.blockId = "minecraft:water";
        new ParticleExpireIfInBlocks(List.of("minecraft:water")).onFrame(blockParticle);
        assertTrue(blockParticle.removed);

        FakeParticle notInBlockParticle = new FakeParticle();
        notInBlockParticle.blockId = "minecraft:air";
        new ParticleExpireIfNotInBlocks(List.of("minecraft:water")).onFrame(notInBlockParticle);
        assertTrue(notInBlockParticle.removed);

        ParticleMotionParametric parametric = new ParticleMotionParametric(
                new io.github.tt432.eyelibmolang.MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(2), MolangValue.getConstant(3)),
                new io.github.tt432.eyelibmolang.MolangValue3(MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO),
                MolangValue.getConstant(30)
        );
        FakeParticle parametricParticle = new FakeParticle();
        parametric.onFrame(parametricParticle);
        assertEquals(new Vector3f(1, 2, 3), parametricParticle.position);
        assertEquals(new Vector3f(0, 1, 0), parametricParticle.velocity);
        assertEquals(30F, parametricParticle.rotation);
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
        public MolangScope molangScope() {
            return scope;
        }

        @Override
        public ParticleBlackboard blackboard() {
            return blackboard;
        }

        @Override
        public float lifetime() {
            return lifetime;
        }

        @Override
        public void setLifetime(float lifetime) {
            this.lifetime = lifetime;
        }

        @Override
        public float age() {
            return age;
        }

        @Override
        public void remove() {
            removed = true;
        }

        @Override
        public void setSpeed(float speed) {
            this.speed = speed;
        }

        @Override
        public void setRotation(float rotation) {
            this.rotation = rotation;
        }

        @Override
        public void setRotationRate(float rotationRate) {
            this.rotationRate = rotationRate;
        }

        @Override
        public float rotation() {
            return rotation;
        }

        @Override
        public float rotationRate() {
            return rotationRate;
        }

        @Override
        public Vector3f position() {
            return position;
        }

        @Override
        public Vector3f velocity() {
            return velocity;
        }

        @Override
        public void setVelocity(Vector3f velocity) {
            this.velocity.set(velocity);
        }

        @Override
        public Optional<String> blockAtPosition() {
            return Optional.ofNullable(blockId);
        }
    }
}