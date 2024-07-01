package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * 所有粒子从发射器附着的实体的轴对齐边界框 (AABB) 中发射，如果没有实体，则从发射器点发射
 *
 * @author TT432
 */
@ParticleComponent(value = "emitter_shape_entity_aabb", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeEntityAABB(
        boolean surfaceOnly,
        Direction direction
) {
    public static final Codec<EmitterShapeEntityAABB> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", new Direction(Direction.Type.OUTWARDS, MolangValue3.ZERO))
                    .forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeEntityAABB::new));
}
