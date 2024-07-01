package io.github.tt432.eyelib.client.particle.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * @author TT432
 */
@ParticleComponent(value = "particle_motion_parametric", type = "particle_motion", target = ComponentTarget.PARTICLE)
public record ParticleMotionParametric(
        MolangValue3 relativePosition,
        MolangValue3 direction,
        MolangValue rotation
) {
    public static final Codec<ParticleMotionParametric> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("relative_position", MolangValue3.ZERO).forGetter(o -> o.relativePosition),
            MolangValue3.CODEC.optionalFieldOf("direction", MolangValue3.ZERO).forGetter(o -> o.direction),
            MolangValue.CODEC.optionalFieldOf("rotation", MolangValue.ZERO).forGetter(o -> o.rotation)
    ).apply(ins, ParticleMotionParametric::new));
}
