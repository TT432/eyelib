package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @param expirationExpression true 时消失，每帧评估
 * @param maxLifetime          超时消失，评估一次，为 0 不消失
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_lifetime_expression", target = ComponentTarget.PARTICLE)
public record ParticleLifetimeExpression(
        MolangValue expirationExpression,
        MolangValue maxLifetime
) implements ParticleParticleComponent {
    public static final Codec<ParticleLifetimeExpression> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("expiration_expression", MolangValue.FALSE_VALUE).forGetter(o -> o.expirationExpression),
            MolangValue.CODEC.optionalFieldOf("max_lifetime", MolangValue.FALSE_VALUE).forGetter(o -> o.maxLifetime)
    ).apply(ins, ParticleLifetimeExpression::new));

    @Override
    public void onStart(BrParticleParticle particle) {
        particle.setLifetime(maxLifetime.eval(particle.molangScope));
    }

    @Override
    public void onFrame(BrParticleParticle particle) {
        if (particle.getAge() >= particle.getLifetime() || expirationExpression.evalAsBool(particle.molangScope)) {
            particle.remove();
        }
    }
}
