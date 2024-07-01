package io.github.tt432.eyelib.client.particle.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;

/**
 * @param linearAcceleration    block/sec/sec，每帧评估
 * @param linearDragCoefficient acceleration = -coefficient * velocity，每帧评估
 * @param rotationAcceleration  角度制
 * @author TT432
 */
@ParticleComponent(value = "particle_motion_dynamic", type = "particle_motion", target = ComponentTarget.PARTICLE)
public record ParticleMotionDynamic(
        MolangValue3 linearAcceleration,
        MolangValue linearDragCoefficient,
        MolangValue rotationAcceleration,
        MolangValue rotationDragCoefficient
) {
    public static final Codec<ParticleMotionDynamic> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("linear_acceleration", MolangValue3.ZERO)
                    .forGetter(o -> o.linearAcceleration),
            MolangValue.CODEC.optionalFieldOf("linear_drag_coefficient", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.linearDragCoefficient),
            MolangValue.CODEC.optionalFieldOf("rotation_acceleration", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.rotationAcceleration),
            MolangValue.CODEC.optionalFieldOf("rotation_drag_coefficient", MolangValue.FALSE_VALUE)
                    .forGetter(o -> o.rotationDragCoefficient)
    ).apply(ins, ParticleMotionDynamic::new));
}
