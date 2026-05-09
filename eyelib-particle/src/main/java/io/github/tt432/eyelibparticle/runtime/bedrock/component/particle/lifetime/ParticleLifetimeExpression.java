package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;

public record ParticleLifetimeExpression(
        MolangValue expirationExpression,
        MolangValue maxLifetime
) implements ParticleParticleComponent {
    public static final Codec<ParticleLifetimeExpression> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("expiration_expression", MolangValue.FALSE_VALUE).forGetter(ParticleLifetimeExpression::expirationExpression),
            MolangValue.CODEC.optionalFieldOf("max_lifetime", MolangValue.FALSE_VALUE).forGetter(ParticleLifetimeExpression::maxLifetime)
    ).apply(ins, ParticleLifetimeExpression::new));

    @Override
    public void onStart(ParticleAccess particle) {
        particle.setLifetime(maxLifetime.eval(particle.molangScope()));
    }

    @Override
    public void onFrame(ParticleAccess particle) {
        if (particle.age() >= particle.lifetime() || expirationExpression.evalAsBool(particle.molangScope())) {
            particle.remove();
        }
    }
}
