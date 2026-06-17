package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeLooping;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class EmitterLifetimeLoopingTest {
    @Test
    void onTickEnablesAndStartsLoopWithinActiveTime() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.age = 0.1F;

        EmitterLifetimeLooping looping = new EmitterLifetimeLooping(MolangValue.getConstant(1), MolangValue.getConstant(2));
        looping.onTick(emitter);

        assertTrue(emitter.enabled);
        assertEquals(1, emitter.loopStarts);
    }

    @Test
    void onTickDisablesEmitterAfterActiveTimeExpires() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.age = 1.5F;

        EmitterLifetimeLooping looping = new EmitterLifetimeLooping(MolangValue.getConstant(1), MolangValue.getConstant(2));
        looping.onTick(emitter);

        assertTrue(emitter.enabled);
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
