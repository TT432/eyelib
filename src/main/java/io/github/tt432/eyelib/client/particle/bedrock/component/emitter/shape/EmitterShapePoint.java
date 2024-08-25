package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * 所有粒子从发射器偏移的一个点发射。
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_shape_point", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapePoint(
        MolangValue3 offset,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapePoint> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue3.CODEC.xmap(
                    mv3 -> new Direction(Direction.Type.CUSTOM, mv3),
                    Direction::custom
            ).optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction)
    ).apply(ins, EmitterShapePoint::new));

    @Override
    public EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return offset::eval;
    }
}
