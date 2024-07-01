package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * 所有粒子基于指定的一组 Molang 表达式发射
 *
 * @param offset    指定从发射器到发射粒子的偏移量。每发射一个粒子时评估一次
 * @param direction 指定粒子的方向。每发射一个粒子时评估一次
 * @author TT432
 */
@ParticleComponent(value = "emitter_shape_custom", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeCustom(
        MolangValue3 offset,
        MolangValue3 direction
) {
    public static final Codec<EmitterShapeCustom> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue3.CODEC.optionalFieldOf("direction", MolangValue3.ZERO).forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeCustom::new));
}
