package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponent;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

public interface EmitterParticleComponent extends ParticleComponent {
    default void onStart(EmitterAccess emitter) {
    }

    default void onTick(EmitterAccess emitter) {
    }

    default void onPreTick(EmitterAccess emitter) {
    }

    default void onLoop(EmitterAccess emitter) {
    }

    default boolean canEmit(EmitterAccess emitter) {
        return true;
    }

    @FunctionalInterface
    interface EvalVector3f {
        Vector3f eval(MolangScope scope);
    }

    @Nullable
    default EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return null;
    }

    default DirectionAccess direction() {
        return DirectionAccess.EMPTY;
    }

    interface DirectionAccess {
        DirectionAccess EMPTY = new DirectionAccess() {
            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public Vector3f getVec(MolangScope scope, Vector3f entityCenter, Vector3f emitPosition) {
                return new Vector3f();
            }
        };

        default boolean isEmpty() {
            return false;
        }

        Vector3f getVec(MolangScope scope, Vector3f entityCenter, Vector3f emitPosition);
    }

    interface EmitterAccess {
        MolangScope molangScope();

        ParticleBlackboard blackboard();

        float age();

        int emitCount();

        void emit();

        void onLoopStart();

        void setEnabled(boolean enabled);

        void remove();
    }
}
