package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate.EmitterRateInstant;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class EmitterRateInstantTest {
    @Test
    void onLoopEmitsSpecifiedNumberOfParticles() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.setEnabled(true);

        new EmitterRateInstant(MolangValue.getConstant(3)).onLoop(emitter);

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
