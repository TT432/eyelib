package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeExpression;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class EmitterLifetimeExpressionTest {
    @Test
    void onTickEnablesAndStartsLoop() {
        FakeEmitter emitter = new FakeEmitter();

        EmitterLifetimeExpression expression = new EmitterLifetimeExpression(MolangValue.TRUE_VALUE, MolangValue.FALSE_VALUE);
        expression.onTick(emitter);

        assertTrue(emitter.enabled);
        assertEquals(1, emitter.loopStarts);
    }

    @Test
    void onTickRemovesEmitterWhenExpirationExpressionTrueWhileInactive() {
        FakeEmitter emitter = new FakeEmitter();

        EmitterLifetimeExpression expression = new EmitterLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.TRUE_VALUE);
        expression.onTick(emitter);

        assertFalse(emitter.enabled);
        assertTrue(emitter.removed);
    }

    @Test
    void onTickDisablesWithoutRemovalWhenBothExpressionsFalse() {
        FakeEmitter emitter = new FakeEmitter();
        emitter.enabled = true;

        EmitterLifetimeExpression expression = new EmitterLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE);
        expression.onTick(emitter);

        assertFalse(emitter.enabled);
        assertFalse(emitter.removed);
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
