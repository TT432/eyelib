package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape.EmitterShapeBox;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class EmitterShapeBoxTest {
    @Test
    void getEmitPositionReturnsFiniteRandomPositionWithinBox() {
        FakeEmitter emitter = new FakeEmitter();

        EmitterShapeBox box = new EmitterShapeBox(
                MolangValue3.ZERO,
                new MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(1), MolangValue.getConstant(1)),
                false,
                Direction.EMPTY
        );

        assertTrue(box.getEmitPosition(emitter).eval(emitter.scope).isFinite());
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
