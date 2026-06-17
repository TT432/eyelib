package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;

/** @author TT432 */
public record EmitterShapeSphere(
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeSphere> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(EmitterShapeSphere::offset),
            MolangValue.CODEC.optionalFieldOf("radius", MolangValue.TRUE_VALUE).forGetter(EmitterShapeSphere::radius),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(EmitterShapeSphere::surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterShapeSphere::direction)
    ).apply(ins, EmitterShapeSphere::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return scope -> {
            float v = emitter.random().nextFloat();
            float w = emitter.random().nextFloat();
            float r = radius.eval(scope);
            if (!surfaceOnly) {
                r *= emitter.random().nextFloat();
            }
            float theta = 2 * (float) Math.PI * v;
            float phi = (float) Math.acos(2 * w - 1);
            return offset.eval(scope).add(
                    r * (float) Math.sin(phi) * (float) Math.cos(theta),
                    r * (float) Math.sin(phi) * (float) Math.sin(theta),
                    r * (float) Math.cos(phi)
            );
        };
    }
}