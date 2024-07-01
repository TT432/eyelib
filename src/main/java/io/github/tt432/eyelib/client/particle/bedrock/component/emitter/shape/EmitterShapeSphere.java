package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * 所有粒子从发射器偏移的一个球体中发射
 *
 * @author TT432
 */
@ParticleComponent(value = "emitter_shape_sphere", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeSphere(
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) {
    public static final Codec<EmitterShapeSphere> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue.CODEC.optionalFieldOf("radius", MolangValue.TRUE_VALUE).forGetter(o -> o.radius),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", new Direction(Direction.Type.OUTWARDS, MolangValue3.ZERO))
                    .forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeSphere::new));
}
