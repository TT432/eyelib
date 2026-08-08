package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;

/** @author TT432 */
public record EmitterLifetimeExpression(
        MolangValue activationExpression,
        MolangValue expirationExpression
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeExpression> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("activation_expression", MolangValue.TRUE_VALUE)
                    .forGetter(EmitterLifetimeExpression::activationExpression),
            MolangValue.CODEC.optionalFieldOf("expiration_expression", MolangValue.FALSE_VALUE)
                    .forGetter(EmitterLifetimeExpression::expirationExpression)
    ).apply(ins, EmitterLifetimeExpression::new));

    private static final String LIFETIME_EXPRESSION_KEY = "lifetime_expression";

    @Override
    public void onTick(EmitterAccess emitter) {
        // BE 语义：expiration 与 activation 独立、每帧各自评估——为真即销毁，
        // 不以 activation 为 false 为前提（Mojang 文档）。嵌套在 else 里会导致
        // activation 恒真的发射器（如 activation=1 + expiration=age>1）永不销毁。
        if (expirationExpression.evalAsBool(emitter.molangScope())) {
            emitter.remove();
            return;
        }
        if (activationExpression.evalAsBool(emitter.molangScope())) {
            emitter.setEnabled(true);

            if (!emitter.blackboard().getOrDefault(LIFETIME_EXPRESSION_KEY, Boolean.class, false)) {
                emitter.blackboard().put(LIFETIME_EXPRESSION_KEY, true);
                emitter.onLoopStart();
            }
        } else {
            emitter.setEnabled(false);
            emitter.blackboard().put(LIFETIME_EXPRESSION_KEY, false);
        }
    }
}