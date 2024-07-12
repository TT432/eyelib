package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

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
@RegisterParticleComponent(value = "emitter_shape_disc", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterDisc(
        MolangValue3 planeNormal,
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
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
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction)
    ).apply(ins, EmitterDisc::new));

    @Override
    public EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return scope -> {
            Vector3f center = this.offset.eval(scope);
            Vector3f normal = this.planeNormal.eval(scope).normalize();
            RandomSource random = emitter.getRandom();

            float r = surfaceOnly
                    ? radius.eval(scope)
                    : radius.eval(scope) * Mth.sqrt(random.nextFloat());

            float angle = random.nextFloat() * Mth.PI * 2;

            float x = r * (float) Math.cos(angle);
            float y = r * (float) Math.sin(angle);

            Vector3f u = new Vector3f().orthogonalize(normal);
            Vector3f v = normal.cross(u, new Vector3f());

            return new Vector3f(center).add(u.mul(x, new Vector3f())).add(v.mul(y, new Vector3f()));
        };
    }
}
