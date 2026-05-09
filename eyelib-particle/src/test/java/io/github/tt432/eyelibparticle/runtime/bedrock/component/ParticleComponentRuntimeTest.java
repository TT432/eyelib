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
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final class FakeParticle implements ParticleParticleComponent.ParticleAccess {
        final MolangScope scope = new MolangScope();
        final ParticleBlackboard blackboard = new ParticleBlackboard();
        float lifetime = 2F;
        float age = 0.5F;
        float speed;
        float rotation;
        float rotationRate;

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
    }
}
