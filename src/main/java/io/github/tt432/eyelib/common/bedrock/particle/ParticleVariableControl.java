package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.math.MolangVariable;

/**
 * @author DustW
 */
public class ParticleVariableControl {
    public void init(MolangParser parser) {
        // Lifetime of the emitter
        parser.register(new MolangVariable("variable.emitter_lifetime", 0));
        // Age of the emitter
        parser.register(new MolangVariable("variable.emitter_age", 0));
        // Random number between 0 and 1, constant per emitter loop
        parser.register(new MolangVariable("variable.emitter_random_1", 0));
        // Random number
        parser.register(new MolangVariable("variable.emitter_random_2", 0));
        // Random number
        parser.register(new MolangVariable("variable.emitter_random_3", 0));
        // Random number
        parser.register(new MolangVariable("variable.emitter_random_4", 0));
        // Lifetime of the particle
        parser.register(new MolangVariable("variable.particle_lifetime", 0));
        // Age of the particle
        parser.register(new MolangVariable("variable.particle_age", 0));
        // Random number between 0 and 1, constant per particle
        parser.register(new MolangVariable("variable.particle_random_1", 0));
        // Random number
        parser.register(new MolangVariable("variable.particle_random_2", 0));
        // Random number
        parser.register(new MolangVariable("variable.particle_random_3", 0));
        // Random number
        parser.register(new MolangVariable("variable.particle_random_4", 0));
        // Scale of the attached entity
        parser.register(new MolangVariable("variable.entity_scale", 0));
    }

    public void setValues(ParticleConstructor particleConstructor) {

    }
}
