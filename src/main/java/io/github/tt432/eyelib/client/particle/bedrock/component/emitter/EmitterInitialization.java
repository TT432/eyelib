package io.github.tt432.eyelib.client.particle.bedrock.component.emitter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author TT432
 */
@ParticleComponent(value = "emitter_initialization", target = ComponentTarget.EMITTER)
public record EmitterInitialization(
        MolangValue creationExpression,
        MolangValue perUpdateExpression
) {
    public static final Codec<EmitterInitialization> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("creation_expression", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.creationExpression),
            MolangValue.CODEC.optionalFieldOf("per_update_expression", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.perUpdateExpression)
    ).apply(ins, EmitterInitialization::new));
}
