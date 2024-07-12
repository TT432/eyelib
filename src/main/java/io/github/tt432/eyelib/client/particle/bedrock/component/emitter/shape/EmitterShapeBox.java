package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.math.Shapes;

/**
 * 所有粒子从指定大小的盒子中发射
 *
 * @param offset         指定从发射器到发射粒子的偏移量。每发射一个粒子时评估一次
 * @param halfDimensions 盒子尺寸。这些是半尺寸，盒子以发射器为中心并沿三个主轴 x/y/z 扩展
 * @param surfaceOnly    仅从盒子表面发射
 * @param direction      指定粒子的方向，默认为 "outwards"。每发射一个粒子时评估一次
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_shape_box", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeBox(
        MolangValue3 offset,
        MolangValue3 halfDimensions,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeBox> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue3.CODEC.fieldOf("half_dimensions").forGetter(o -> o.halfDimensions),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeBox::new));

    @Override
    public EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return scope -> Shapes.getRandomPointInAABB(emitter.getRandom(), surfaceOnly,
                offset.eval(scope), halfDimensions.eval(scope));
    }
}
