package io.github.tt432.eyelib.client.particle.bedrock.component.emitter;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape.Direction;
import io.github.tt432.eyelib.molang.MolangScope;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public interface EmitterParticleComponent extends ParticleComponent {
    default void onStart(BrParticleEmitter emitter) {
    }

    default void onTick(BrParticleEmitter emitter) {
    }

    default void onPreTick(BrParticleEmitter emitter) {
    }

    default void onLoop(BrParticleEmitter emitter) {
    }

    default boolean canEmit(BrParticleEmitter emitter) {
        return true;
    }

    @FunctionalInterface
    interface EvalVector3f {
        Vector3f eval(MolangScope scope);
    }

    default EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return null;
    }

    default Direction direction() {
        return Direction.EMPTY;
    }
}
