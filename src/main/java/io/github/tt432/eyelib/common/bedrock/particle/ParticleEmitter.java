package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterInitialization;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLifetimeEvents;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.lifetime.EmitterLifetimeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate.EmitterRateComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape.EmitterShapeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.Particle;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
public class ParticleEmitter {
    MolangVariableScope scope;

    EmitterInitialization initialization;
    EmitterLifetimeEvents lifetimeEvents;
    EmitterLocalSpace localSpace;
    EmitterLifetimeComponent lifeTimeComponent;
    EmitterRateComponent rateComponent;
    EmitterShapeComponent shapeComponent;

    ParticleConstructor constructor;

    List<ParticleComponent> components;

    @Builder
    public ParticleEmitter(MolangVariableScope scope, EmitterInitialization initialization,
                           EmitterLifetimeEvents lifetimeEvents, EmitterLocalSpace localSpace,
                           EmitterLifetimeComponent lifeTimeComponent, EmitterRateComponent rateComponent,
                           EmitterShapeComponent shapeComponent, ParticleConstructor constructor) {
        this.scope = scope;
        this.initialization = initialization;
        this.lifetimeEvents = lifetimeEvents;
        this.localSpace = localSpace;
        this.lifeTimeComponent = lifeTimeComponent;
        this.rateComponent = rateComponent;
        this.shapeComponent = shapeComponent;
        this.constructor = constructor;

        components = new ArrayList<>();
        addComponentNotNull(initialization);
        addComponentNotNull(lifetimeEvents);
        addComponentNotNull(localSpace);
        addComponentNotNull(lifeTimeComponent);
        addComponentNotNull(rateComponent);
        addComponentNotNull(shapeComponent);
    }

    void addComponentNotNull(ParticleComponent component) {
        if (component != null)
            components.add(component);
    }

    List<ParticleInstance> instances = new ArrayList<>();
    int lifeTime;

    public void tick() {
        if (lifeTime == 0) {
            start();
        }

        lifeTime++;
        loopStart();
    }

    public void render() {
        update();
        //TODO 判断可以发射粒子调用 emit 并发射，将 ParticleInstance 放入 instances
    }

    private void start() {
        for (ParticleComponent component : components) {
            component.evaluateStart(scope);
        }
    }

    private void loopStart() {
        for (ParticleComponent component : components) {
            component.evaluateLoopStart(scope);
        }
    }

    private void update() {
        for (ParticleComponent component : components) {
            component.evaluatePerUpdate(scope);
        }
    }

    private void emit() {
        for (ParticleComponent component : components) {
            component.evaluatePerEmit(scope);
        }
    }

    public static ParticleEmitter from(Particle particle) {
        var components = particle.getEffect().getComponents();

        return ParticleEmitter.builder()
                .scope(particle.getScope().copy())
                .initialization(components.getByClass(EmitterInitialization.class))
                .lifetimeEvents(components.getByClass(EmitterLifetimeEvents.class))
                .localSpace(components.getByClass(EmitterLocalSpace.class))
                .lifeTimeComponent(components.getByClass(EmitterLifetimeComponent.class))
                .rateComponent(components.getByClass(EmitterRateComponent.class))
                .shapeComponent(components.getByClass(EmitterShapeComponent.class))
                .constructor(new ParticleConstructor())
                .build();
    }
}
