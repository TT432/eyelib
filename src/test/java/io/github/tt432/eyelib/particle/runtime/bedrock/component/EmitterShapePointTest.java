package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape.EmitterShapePoint;
import io.github.tt432.eyelib.particle.runtime.support.ParticleBlackboard;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class EmitterShapePointTest {
    @Test
    void getEmitPositionReturnsExactOffset() {
        FakeEmitter emitter = new FakeEmitter();

        EmitterShapePoint point = new EmitterShapePoint(
                new MolangValue3(MolangValue.getConstant(1), MolangValue.getConstant(2), MolangValue.getConstant(3)),
                Direction.EMPTY
        );

        Vector3f position = point.getEmitPosition(emitter).eval(emitter.scope);
        assertEquals(new Vector3f(1, 2, 3), position);
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
