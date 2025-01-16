package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.Blackboard;

/**
 * 当 {@link #activationExpression} 非零时，发射器将“开启”；当其为零时，发射器将“关闭”。这对于通过实体变量驱动附加到实体的发射器的情况非常有用。
 *
 * @param activationExpression 当表达式非零时，发射器会发射粒子。每帧评估一次
 * @param expirationExpression 当表达式非零时，发射器将过期。每帧评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_lifetime_expression", type = "emitter_lifetime", target = ComponentTarget.EMITTER)
public record EmitterLifetimeExpression(
        MolangValue activationExpression,
        MolangValue expirationExpression
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeExpression> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("activation_expression", MolangValue.TRUE_VALUE)
                    .forGetter(o -> o.activationExpression),
            MolangValue.CODEC.optionalFieldOf("expiration_expression", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.expirationExpression)
    ).apply(ins, EmitterLifetimeExpression::new));

    private static final String lifetimeExpressionKey = "lifetime_expression";

    @Override
    public void onTick(BrParticleEmitter emitter) {
        Blackboard blackboard = emitter.blackboard;
        if (activationExpression.evalAsBool(emitter.molangScope)) {
            emitter.setEnabled(true);

            if (!blackboard.getOrCreate(lifetimeExpressionKey, false)) {
                blackboard.put(lifetimeExpressionKey, true);
                emitter.onLoopStart();
            }
        } else if (expirationExpression.evalAsBool(emitter.molangScope)) {
            emitter.setEnabled(false);
            blackboard.put(lifetimeExpressionKey, false);
        }
    }
}
