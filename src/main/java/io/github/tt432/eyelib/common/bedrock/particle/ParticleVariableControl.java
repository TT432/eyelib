package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticleVariableControl {
    public static void setEmitterVariable(MolangVariableScope scope) {
        // Lifetime of the emitter
        scope.setVariable("variable.emitter_lifetime", s -> s.getValue("active_time"));
        // Age of the emitter
        scope.setVariable("variable.emitter_age", s -> {
            ParticleEmitter emitter = s.getDataSource().get(ParticleEmitter.class);
            return (emitter.age + emitter.partialTicks) / 20D;
        });
        // Random number between 0 and 1, constant per emitter loop
        scope.setVariable("variable.emitter_random_1", s -> s.getDataSource().get(ParticleEmitter.class).random1);
        // Random number
        scope.setVariable("variable.emitter_random_2", s -> s.getDataSource().get(ParticleEmitter.class).random2);
        // Random number
        scope.setVariable("variable.emitter_random_3", s -> s.getDataSource().get(ParticleEmitter.class).random3);
        // Random number
        scope.setVariable("variable.emitter_random_4", s -> s.getDataSource().get(ParticleEmitter.class).random4);
        scope.setVariable("variable.emitter_particles_num", s -> (double) s.getDataSource().get(ParticleEmitter.class).particles.size());
    }

    public static void setParticleVariable(MolangVariableScope scope) {
        // Lifetime of the particle
        scope.setVariable("variable.particle_lifetime", s -> s.getDataSource().get(ParticleInstance.class).maxLifetime);
        // Age of the particle
        scope.setVariable("variable.particle_age", s ->
                (s.getDataSource().get(ParticleInstance.class).age + s.getDataSource().get(ParticleEmitter.class).partialTicks) / 20D);
        // Random number between 0 and 1, constant per particle
        scope.setVariable("variable.particle_random_1", s -> s.getDataSource().get(ParticleInstance.class).random1);
        // Random number
        scope.setVariable("variable.particle_random_2", s -> s.getDataSource().get(ParticleInstance.class).random2);
        // Random number
        scope.setVariable("variable.particle_random_3", s -> s.getDataSource().get(ParticleInstance.class).random3);
        // Random number
        scope.setVariable("variable.particle_random_4", s -> s.getDataSource().get(ParticleInstance.class).random4);
    }

    public static void setEntityVariable(MolangVariableScope scope) {
        // Scale of the attached entity
        scope.setVariable("variable.entity_scale", s -> 1D);
    }
}
