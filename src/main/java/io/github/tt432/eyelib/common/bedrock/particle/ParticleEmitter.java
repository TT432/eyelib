package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterInitialization;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLifetimeEvents;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.lifetime.EmitterLifetimeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate.EmitterRateComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape.EmitterShapeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.Particle;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.Builder;

/**
 * @author DustW
 */
@Builder
public class ParticleEmitter {
    MolangVariableScope scope;

    EmitterInitialization initialization;
    EmitterLifetimeEvents lifetimeEvents;
    EmitterLocalSpace localSpace;
    EmitterLifetimeComponent lifeTimeComponent;
    EmitterRateComponent rateComponent;
    EmitterShapeComponent shapeComponent;

    ParticleConstructor constructor;

    public void start() {
        if (initialization != null) {
            initialization.getCreation().evaluateWithCache("creation", scope);
        }

        if (lifeTimeComponent != null) {
            lifeTimeComponent.evaluateStart(scope);
        }
    }

    public void loopStart() {
        if (lifeTimeComponent != null) {
            lifeTimeComponent.evaluateLoopStart(scope);
        }

        if (rateComponent != null) {
            rateComponent.evaluateLoopStart(scope);
        }
    }

    public void update() {
        if (initialization != null) {
            initialization.getPerUpdate().evaluateWithCache("per_update", scope);
        }

        if (lifeTimeComponent != null) {
            lifeTimeComponent.evaluatePerUpdate(scope);
        }
    }

    public void emit() {
        if (rateComponent != null) {
            rateComponent.evaluatePerEmit(scope);
        }

        if (shapeComponent != null) {
            shapeComponent.evaluatePerEmit(scope);
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
                .build();
    }
}
