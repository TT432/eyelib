package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;

public record EmitterInitialization(
        MolangValue creationExpression,
        MolangValue perUpdateExpression
) implements EmitterParticleComponent {
    public static final Codec<EmitterInitialization> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("creation_expression", MolangValue.FALSE_VALUE)
                    .forGetter(EmitterInitialization::creationExpression),
            MolangValue.CODEC.optionalFieldOf("per_update_expression", MolangValue.FALSE_VALUE)
                    .forGetter(EmitterInitialization::perUpdateExpression)
    ).apply(ins, EmitterInitialization::new));

    @Override
    public void onStart(EmitterAccess emitter) {
        creationExpression.eval(emitter.molangScope());
    }

    @Override
    public void onPreTick(EmitterAccess emitter) {
        perUpdateExpression.eval(emitter.molangScope());
    }
}
