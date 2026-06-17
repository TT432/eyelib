package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.concurrent.ThreadLocalRandom;

/** @author TT432 */
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

    default Direction direction() {
        return Direction.EMPTY;
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

        default RandomGenerator random() {
            return ThreadLocalRandom.current();
        }

        default Optional<Bounds> entityBounds() {
            return Optional.empty();
        }

        record Bounds(Vector3f center, Vector3f halfDimensions) {
            public Bounds {
                center = new Vector3f(center);
                halfDimensions = new Vector3f(halfDimensions);
            }
        }
    }
}