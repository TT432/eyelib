package io.github.tt432.eyelib.client.particle.bedrock.component.emitter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_initialization", target = ComponentTarget.EMITTER)
public record EmitterInitialization(
        MolangValue creationExpression,
        MolangValue perUpdateExpression
) implements EmitterParticleComponent {
    public static final Codec<EmitterInitialization> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("creation_expression", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.creationExpression),
            MolangValue.CODEC.optionalFieldOf("per_update_expression", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.perUpdateExpression)
    ).apply(ins, EmitterInitialization::new));

    @Override
    public void onStart(BrParticleEmitter emitter) {
        creationExpression.eval(emitter.molangScope);
    }

    @Override
    public void onPreTick(BrParticleEmitter emitter) {
        perUpdateExpression.eval(emitter.molangScope);
    }
}
