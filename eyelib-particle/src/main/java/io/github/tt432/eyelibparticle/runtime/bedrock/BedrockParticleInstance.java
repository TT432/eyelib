package io.github.tt432.eyelibparticle.runtime.bedrock;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import io.github.tt432.eyelibparticle.runtime.support.ParticleTimer;
import org.joml.Vector3f;

/**
 * Module-owned particle instance lifecycle state.
 */
public final class BedrockParticleInstance {
    private final BedrockParticleEmitter emitter;
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final MolangScope molangScope = new MolangScope();
    private final ParticleBlackboard blackboard = new ParticleBlackboard();
    private final ParticleTimer timer;
    private float speed;
    private boolean removed;

    BedrockParticleInstance(BedrockParticleEmitter emitter) {
        this.emitter = emitter;
        timer = new ParticleTimer(new io.github.tt432.eyelibparticle.runtime.ParticleRuntimeServices.TimeSource() {
            @Override
            public int ticks() {
                return emitter.environment().ticks();
            }

            @Override
            public float partialTick() {
                return emitter.environment().partialTick();
            }
        });
        timer.start();
        molangScope.setParent(emitter.molangScope());
        molangScope.getHostContext().put(BedrockParticleInstance.class, this);
    }

    public BedrockParticleEmitter emitter() {
        return emitter;
    }

    public Vector3f position() {
        return position;
    }

    public Vector3f velocity() {
        return velocity;
    }

    public MolangScope molangScope() {
        return molangScope;
    }

    public ParticleBlackboard blackboard() {
        return blackboard;
    }

    public float speed() {
        return speed;
    }

    public boolean removed() {
        return removed;
    }

    public void remove() {
        if (!removed) {
            removed = true;
            emitter.onParticleRemove();
        }
    }
}
