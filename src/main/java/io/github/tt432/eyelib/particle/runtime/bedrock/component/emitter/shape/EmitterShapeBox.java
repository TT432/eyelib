package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import org.joml.Vector3f;

/** @author TT432 */
public record EmitterShapeBox(
        MolangValue3 offset,
        MolangValue3 halfDimensions,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeBox> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(EmitterShapeBox::offset),
            MolangValue3.CODEC.fieldOf("half_dimensions").forGetter(EmitterShapeBox::halfDimensions),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(EmitterShapeBox::surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterShapeBox::direction)
    ).apply(ins, EmitterShapeBox::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return scope -> randomPointInBox(emitter, surfaceOnly, offset.eval(scope), halfDimensions.eval(scope));
    }

    static Vector3f randomPointInBox(EmitterAccess emitter, boolean surfaceOnly, Vector3f center, Vector3f halfDimensions) {
        float x = randomBetween(emitter, -halfDimensions.x(), halfDimensions.x());
        float y = randomBetween(emitter, -halfDimensions.y(), halfDimensions.y());
        float z = randomBetween(emitter, -halfDimensions.z(), halfDimensions.z());

        if (surfaceOnly) {
            int axis = emitter.random().nextInt(3);
            float sign = emitter.random().nextBoolean() ? 1F : -1F;
            if (axis == 0) x = halfDimensions.x() * sign;
            else if (axis == 1) y = halfDimensions.y() * sign;
            else z = halfDimensions.z() * sign;
        }

        return new Vector3f(center).add(x, y, z);
    }

    private static float randomBetween(EmitterAccess emitter, float min, float max) {
        return min + emitter.random().nextFloat() * (max - min);
    }
}