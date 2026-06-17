package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate.EmitterRateSteady;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class EmitterRateSteadyTest {
    @Test
    void onTickEmitsAtSpecifiedRate() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        EmitterRateSteady steady = new EmitterRateSteady(MolangValue.getConstant(2), MolangValue.getConstant(10));
        steady.onTick(emitter);

        assertEquals(1, emitter.emitCalls);
    }

    @Test
    void onTickAccumulatesDeltaAndEmitsPeriodically() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        EmitterRateSteady steady = new EmitterRateSteady(MolangValue.getConstant(2), MolangValue.getConstant(10));
        steady.onTick(emitter);
        emitter.age = 0.25F;
        steady.onTick(emitter);

        assertEquals(2, emitter.emitCalls);
    }

    @Test
    void onTickRespectsMaxParticlesPerLoop() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        EmitterRateSteady steady = new EmitterRateSteady(MolangValue.getConstant(2), MolangValue.getConstant(10));
        steady.onTick(emitter);
        emitter.age = 0.25F;
        steady.onTick(emitter);
        emitter.age = 0.6F;
        steady.onTick(emitter);

        assertEquals(2, emitter.emitCalls);
    }

    @Test
    void onLoopResetsAccumulatorForNewLoop() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        EmitterRateSteady steady = new EmitterRateSteady(MolangValue.getConstant(2), MolangValue.getConstant(10));
        steady.onTick(emitter);
        emitter.age = 0.25F;
        steady.onTick(emitter);
        steady.onLoop(emitter);
        emitter.age = 0.7F;
        steady.onTick(emitter);

        assertEquals(3, emitter.emitCalls);
    }

    private static final class FakeEmitter implements EmitterParticleComponent.EmitterAccess {
        final MolangScope scope = new MolangScope();
        final ParticleBlackboard blackboard = new ParticleBlackboard();
        int emitCalls;
        int emitCount;
        int loopStarts;
        float age;
        boolean enabled;
        boolean removed;

        @Override
        public MolangScope molangScope() { return scope; }
        @Override
        public ParticleBlackboard blackboard() { return blackboard; }
        @Override
        public float age() { return age; }
        @Override
        public int emitCount() { return emitCount; }
        @Override
        public void emit() { emitCalls++; emitCount++; }
        @Override
        public void onLoopStart() { loopStarts++; }
        @Override
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        @Override
        public void remove() { removed = true; }
    }
}
