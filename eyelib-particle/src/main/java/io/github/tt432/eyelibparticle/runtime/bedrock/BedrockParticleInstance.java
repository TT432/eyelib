package io.github.tt432.eyelibparticle.runtime.bedrock;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentManager;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import io.github.tt432.eyelibparticle.runtime.support.ParticleTimer;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Module-owned particle instance lifecycle state.
 */
public final class BedrockParticleInstance implements ParticleParticleComponent.ParticleAccess {
    private final BedrockParticleEmitter emitter;
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final MolangScope molangScope = new MolangScope();
    private final ParticleBlackboard blackboard = new ParticleBlackboard();
    private final ParticleTimer timer;
    private final List<ParticleParticleComponent> components;
    private final float random1;
    private final float random2;
    private final float random3;
    private final float random4;
    private float speed;
    private float rotation;
    private float rotationRate;
    private float lifetime;
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
        random1 = emitter.random().nextFloat();
        random2 = emitter.random().nextFloat();
        random3 = emitter.random().nextFloat();
        random4 = emitter.random().nextFloat();
        molangScope.setParent(emitter.molangScope());
        molangScope.getHostContext().put(BedrockParticleInstance.class, this);
        emitter.definition().curves().forEach((key, curve) -> molangScope.set(key, () -> BedrockParticleEmitter.calculateCurve(curve, molangScope)));
        molangScope.set("variable.particle_age", this::age);
        molangScope.set("variable.particle_lifetime", this::lifetime);
        molangScope.set("variable.particle_random_1", this::random1);
        molangScope.set("variable.particle_random_2", this::random2);
        molangScope.set("variable.particle_random_3", this::random3);
        molangScope.set("variable.particle_random_4", this::random4);
        components = ParticleComponentManager.particleComponents(emitter.definition());
        components.forEach(component -> component.onStart(this));
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

    @Override
    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    public MolangScope molangScope() {
        return molangScope;
    }

    public ParticleBlackboard blackboard() {
        return blackboard;
    }

    @Override
    public float lifetime() {
        return lifetime;
    }

    @Override
    public void setLifetime(float lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public float age() {
        return Math.min(lifetime, timer.realSec());
    }

    public float speed() {
        return speed;
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float rotation() {
        return rotation;
    }

    @Override
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float rotationRate() {
        return rotationRate;
    }

    @Override
    public void setRotationRate(float rotationRate) {
        this.rotationRate = rotationRate;
    }

    public float random1() {
        return random1;
    }

    public float random2() {
        return random2;
    }

    public float random3() {
        return random3;
    }

    public float random4() {
        return random4;
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

    public void onRenderFrame() {
        components.forEach(component -> component.onFrame(this));
    }

    @Override
    public Vector3f emitterPosition() {
        return emitter.position();
    }

    @Override
    public Optional<String> blockAtPosition() {
        return emitter.environment().blockAtPosition(position);
    }
}
