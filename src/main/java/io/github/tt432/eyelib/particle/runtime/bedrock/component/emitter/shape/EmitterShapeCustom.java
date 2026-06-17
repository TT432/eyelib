package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;

/** @author TT432 */
public record EmitterShapeCustom(
        MolangValue3 offset,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeCustom> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(EmitterShapeCustom::offset),
            MolangValue3.CODEC.xmap(value -> new Direction(Direction.Type.CUSTOM, value), Direction::custom)
                    .optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterShapeCustom::direction)
    ).apply(ins, EmitterShapeCustom::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return offset::eval;
    }
}