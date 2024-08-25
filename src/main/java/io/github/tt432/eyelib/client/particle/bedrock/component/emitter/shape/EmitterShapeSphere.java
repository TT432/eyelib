package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.emitter.EmitterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 所有粒子从发射器偏移的一个球体中发射
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "emitter_shape_sphere", type = "emitter_shape", target = ComponentTarget.EMITTER)
public record EmitterShapeSphere(
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeSphere> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(o -> o.offset),
            MolangValue.CODEC.optionalFieldOf("radius", MolangValue.TRUE_VALUE).forGetter(o -> o.radius),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(o -> o.surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction)
    ).apply(ins, EmitterShapeSphere::new));

    @Override
    public EvalVector3f getEmitPosition(BrParticleEmitter emitter) {
        return scope -> {
            RandomSource random = emitter.getRandom();

            var v = random.nextFloat();
            var w = random.nextFloat();

            var r = radius.eval(scope);

            if (surfaceOnly) {
                r *= random.nextFloat();
            }

            var theta = 2 * EyeMath.PI * v;
            var phi = (float) Math.acos(2 * w - 1);

            return offset.eval(scope).add(
                    r * Mth.sin(phi) * Mth.cos(theta),
                    r * Mth.sin(phi) * Mth.sin(theta),
                    r * Mth.cos(phi)
            );
        };
    }
}
