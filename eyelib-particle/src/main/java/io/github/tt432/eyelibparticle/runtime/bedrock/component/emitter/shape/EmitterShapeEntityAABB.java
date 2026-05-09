package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import org.joml.Vector3f;

public record EmitterShapeEntityAABB(
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterShapeEntityAABB> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(EmitterShapeEntityAABB::surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterShapeEntityAABB::direction)
    ).apply(ins, EmitterShapeEntityAABB::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return scope -> emitter.entityBounds()
                .map(bounds -> EmitterShapeBox.randomPointInBox(emitter, surfaceOnly, bounds.center(), bounds.halfDimensions()))
                .orElse(new Vector3f());
    }
}
