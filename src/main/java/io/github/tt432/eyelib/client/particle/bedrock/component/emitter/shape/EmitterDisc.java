package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * 该组件使用圆盘形状生成粒子，粒子可以在形状内或其外边缘生成。
 *
 * @param planeNormal 指定圆盘平面的法线，圆盘将垂直于此方向
 * @param offset      指定从发射器到发射粒子的偏移量。每发射一个粒子时评估一次
 * @param radius      圆盘半径。每发射一个粒子时评估一次
 * @param surfaceOnly 仅从圆盘边缘发射
 * @param direction   指定粒子的方向，默认为 "outwards"。每发射一个粒子时评估一次
 * @author TT432
 */
@ParticleComponent(value = "emitter_shape_disc", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterDisc(
        MolangValue3 planeNormal,
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) {
    public static final Codec<EmitterDisc> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.either(Codec.STRING.xmap(s -> switch (s) {
                                case "x" -> MolangValue3.AXIS_X;
                                case "z" -> MolangValue3.AXIS_Z;
                                default -> MolangValue3.AXIS_Y;
                            }, mv3 -> {
                                if (mv3.equals(MolangValue3.AXIS_X)) return "x";
                                else if (mv3.equals(MolangValue3.AXIS_Z)) return "z";
                                else return "y";
                            }),
                            MolangValue3.CODEC)
                    .xmap(Either::unwrap, Either::right)
                    .optionalFieldOf("plane_normal", MolangValue3.AXIS_Y).forGetter(o -> o.planeNormal),
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue.CODEC.optionalFieldOf("radius", MolangValue.TRUE_VALUE).forGetter(o -> o.radius),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", new Direction(Direction.Type.OUTWARDS, MolangValue3.ZERO))
                    .forGetter(o -> o.direction)
    ).apply(ins, EmitterDisc::new));

}
