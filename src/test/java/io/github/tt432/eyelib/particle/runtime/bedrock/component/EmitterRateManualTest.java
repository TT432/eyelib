package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate.EmitterRateManual;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class EmitterRateManualTest {
    @Test
    void canEmitReturnsFalseWhenUnderMaxParticles() {
        FakeEmitter emitter = new FakeEmitter();
        EmitterRateManual manual = new EmitterRateManual(MolangValue.getConstant(3));

        assertTrue(manual.canEmit(emitter));
    }

    @Test
    void canEmitReturnsTrueWhenAboveMaxParticles() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.emitCount = 2;
        EmitterRateManual manual = new EmitterRateManual(MolangValue.getConstant(3));

        assertTrue(manual.canEmit(emitter));
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
