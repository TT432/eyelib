package io.github.tt432.eyelib.client.particle.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import org.joml.Vector3f;

/**
 * todo
 *
 * @param linearAcceleration    block/sec/sec，每帧评估
 * @param linearDragCoefficient acceleration = -coefficient * velocity，每帧评估
 * @param rotationAcceleration  角度制
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_motion_dynamic", type = "particle_motion", target = ComponentTarget.PARTICLE)
public record ParticleMotionDynamic(
        MolangValue3 linearAcceleration,
        MolangValue linearDragCoefficient,
        MolangValue rotationAcceleration,
        MolangValue rotationDragCoefficient
) implements ParticleParticleComponent {
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

    public static class Data {
        float lastAge;
    }

    @Override
    public void onFrame(BrParticleParticle particle) {
        var blackboard = particle.getBlackboard();
        Data motionDynamic = blackboard.getOrCreate("motion_dynamic", new Data());
        float age = particle.getAge();
        float deltaTime = age - motionDynamic.lastAge;
        MolangScope scope = particle.molangScope;

        Vector3f acceleration = linearAcceleration.eval(scope);
        float dragCoefficient = linearDragCoefficient.eval(scope);
        Vector3f velocity = particle.getVelocity();
        Vector3f dragForce = velocity.mul(-dragCoefficient, new Vector3f());
        acceleration.add(dragForce);
        velocity.add(acceleration.mul(deltaTime));
        Vector3f position = particle.getPosition();
        position.add(velocity.mul(deltaTime, new Vector3f()).div(16));

        float rotAcceleration = rotationAcceleration.eval(scope);
        float rotDragCoefficient = rotationDragCoefficient.eval(scope);
        float angularVelocity = particle.getRotationRate();
        float rotDragForce = -rotDragCoefficient * angularVelocity;
        float totalRotAcceleration = rotAcceleration + rotDragForce;
        angularVelocity += totalRotAcceleration * deltaTime;
        float rotation = particle.getRotation();
        rotation += angularVelocity * deltaTime;
        particle.setRotationRate(angularVelocity);
        particle.setRotation(rotation);
        motionDynamic.lastAge = age;
    }
}
