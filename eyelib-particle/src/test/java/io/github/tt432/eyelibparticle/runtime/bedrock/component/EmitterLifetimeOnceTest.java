package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeOnce;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class EmitterLifetimeOnceTest {
    @Test
    void onStartEnablesEmitter() {
        FakeEmitter emitter = new FakeEmitter();
        EmitterLifetimeOnce once = new EmitterLifetimeOnce(MolangValue.getConstant(1));

        once.onStart(emitter);
        once.onTick(emitter);

        assertEquals(1, emitter.loopStarts);
    }

    @Test
    void onTickRemovesEmitterAfterActiveTime() {
        FakeEmitter emitter = new FakeEmitter();
        EmitterLifetimeOnce once = new EmitterLifetimeOnce(MolangValue.getConstant(1));
        once.onStart(emitter);
        emitter.age = 1.1F;
        once.onTick(emitter);

        assertTrue(emitter.removed);
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
