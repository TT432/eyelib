package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeExpression;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeLooping;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeOnce;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapeBox;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.EmitterShapePoint;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateInstant;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateManual;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.rate.EmitterRateSteady;
import io.github.tt432.eyelibmolang.MolangValue3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmitterComponentRuntimeTest {
    @Test
    void componentManagerDecodesEmitterRateAndLifetimeFromRawComponents() {
        ParticleDefinition definition = definitionWithComponents();

        List<EmitterParticleComponent> components = ParticleComponentManager.emitterComponents(definition);

        assertTrue(ParticleComponentManager.classSourceMentionsRawComponents(), "manager must dispatch from rawComponents");
        assertTrue(components.stream().anyMatch(EmitterRateInstant.class::isInstance));
        assertTrue(components.stream().anyMatch(EmitterRateManual.class::isInstance));
        assertTrue(components.stream().anyMatch(EmitterRateSteady.class::isInstance));
        assertTrue(components.stream().anyMatch(EmitterLifetimeOnce.class::isInstance));
    }

    @Test
    void instantManualAndSteadyRateComponentsPreserveEmissionGating() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        new EmitterRateInstant(MolangValue.getConstant(3)).onLoop(emitter);
        assertEquals(3, emitter.emitCalls);

        EmitterRateManual manual = new EmitterRateManual(MolangValue.getConstant(3));
        assertFalse(manual.canEmit(emitter));
        emitter.emitCount = 2;
        assertTrue(manual.canEmit(emitter));

        EmitterRateSteady steady = new EmitterRateSteady(MolangValue.getConstant(2), MolangValue.getConstant(10));
        steady.onTick(emitter);
        assertEquals(4, emitter.emitCalls);
        emitter.age = 0.25F;
        steady.onTick(emitter);
        assertEquals(5, emitter.emitCalls);
        emitter.age = 0.6F;
        steady.onTick(emitter);
        assertEquals(5, emitter.emitCalls);

        steady.onLoop(emitter);
        emitter.age = 0.7F;
        steady.onTick(emitter);
        assertEquals(6, emitter.emitCalls);
    }

    @Test
    void onceLoopingAndExpressionLifetimeComponentsPreserveLifecycleEffects() {
        FakeEmitter emitter = new FakeEmitter();

        EmitterLifetimeOnce once = new EmitterLifetimeOnce(MolangValue.getConstant(1));
        once.onStart(emitter);
        assertTrue(emitter.enabled);
        once.onTick(emitter);
        assertEquals(1, emitter.loopStarts);
        emitter.age = 1.1F;
        once.onTick(emitter);
        assertTrue(emitter.removed);

        FakeEmitter loopingEmitter = new FakeEmitter();
        loopingEmitter.age = 0.1F;
        EmitterLifetimeLooping looping = new EmitterLifetimeLooping(MolangValue.getConstant(1), MolangValue.getConstant(2));
        looping.onTick(loopingEmitter);
        assertTrue(loopingEmitter.enabled);
        assertEquals(1, loopingEmitter.loopStarts);
        loopingEmitter.age = 1.5F;
        looping.onTick(loopingEmitter);
        assertFalse(loopingEmitter.enabled);

        FakeEmitter expressionEmitter = new FakeEmitter();
        EmitterLifetimeExpression expression = new EmitterLifetimeExpression(MolangValue.TRUE_VALUE, MolangValue.FALSE_VALUE);
        expression.onTick(expressionEmitter);
        assertTrue(expressionEmitter.enabled);
        assertEquals(1, expressionEmitter.loopStarts);
    }

    @Test
    void localSpaceAndShapeComponentsPreservePositionEvaluationAndDirection() {
        assertFalse(EmitterLocalSpace.EMPTY.position());
        assertFalse(EmitterLocalSpace.EMPTY.rotation());
        assertFalse(EmitterLocalSpace.EMPTY.velocity());

        FakeEmitter emitter = new FakeEmitter();
        EmitterShapePoint point = new EmitterShapePoint(
                new MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(2), MolangValue.getConstant(3)),
                Direction.EMPTY
        );
        Vector3f pointPosition = point.getEmitPosition(emitter).eval(emitter.scope);
        assertEquals(new Vector3f(1, 2, 3), pointPosition);

        EmitterShapeBox box = new EmitterShapeBox(
                MolangValue3.ZERO,
                new MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(1), MolangValue.getConstant(1)),
                false,
                Direction.EMPTY
        );
        assertTrue(box.getEmitPosition(emitter).eval(emitter.scope).isFinite());
        assertTrue(new Direction(Direction.Type.OUTWARDS, null)
                .getVec(emitter.scope, new Vector3f(), new Vector3f(1, 0, 0)).x() > 0);
    }

    private static ParticleDefinition definitionWithComponents() {
        String json = """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "eyelib:test_particle",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "textures/particle/test"
                      }
                    },
                    "components": {
                      "minecraft:emitter_rate_instant": { "num_particles": 3 },
                      "minecraft:emitter_rate_manual": { "max_particles": 7 },
                      "minecraft:emitter_rate_steady": { "spawn_rate": 2, "max_particles": 8 },
                      "minecraft:emitter_lifetime_once": { "active_time": 1 }
                    }
                  }
                }
                """;
        return ParticleDefinitionAdapter.fromSchema(
                io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                        .getOrThrow(false, AssertionError::new)
        ).getOrThrow(false, AssertionError::new);
    }

    private static final class FakeEmitter implements EmitterParticleComponent.EmitterAccess {
        final MolangScope scope = new MolangScope();
        final io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard blackboard = new io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard();
        int emitCalls;
        int emitCount;
        int loopStarts;
        float age;
        boolean enabled;
        boolean removed;

        @Override
        public MolangScope molangScope() {
            return scope;
        }

        @Override
        public io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard blackboard() {
            return blackboard;
        }

        @Override
        public float age() {
            return age;
        }

        @Override
        public int emitCount() {
            return emitCount;
        }

        @Override
        public void emit() {
            emitCalls++;
            emitCount++;
        }

        @Override
        public void onLoopStart() {
            loopStarts++;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void remove() {
            removed = true;
        }
    }
}
