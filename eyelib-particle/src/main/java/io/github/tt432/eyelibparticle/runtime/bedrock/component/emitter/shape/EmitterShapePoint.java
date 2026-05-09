package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;

public record EmitterShapePoint(
        MolangValue3 offset,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapePoint> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(EmitterShapePoint::offset),
            MolangValue3.CODEC.xmap(value -> new Direction(Direction.Type.CUSTOM, value), Direction::custom)
                    .optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterShapePoint::direction)
    ).apply(ins, EmitterShapePoint::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return offset::eval;
    }
}
