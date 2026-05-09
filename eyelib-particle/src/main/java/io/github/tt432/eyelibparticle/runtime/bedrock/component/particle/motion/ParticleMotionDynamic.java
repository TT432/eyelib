package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.motion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import org.joml.Vector3f;

public record ParticleMotionDynamic(
        MolangValue3 linearAcceleration,
        MolangValue linearDragCoefficient,
        MolangValue rotationAcceleration,
        MolangValue rotationDragCoefficient
) implements ParticleParticleComponent {
    public static final Codec<ParticleMotionDynamic> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue3.CODEC.optionalFieldOf("linear_acceleration", MolangValue3.ZERO).forGetter(ParticleMotionDynamic::linearAcceleration),
            MolangValue.CODEC.optionalFieldOf("linear_drag_coefficient", MolangValue.FALSE_VALUE).forGetter(ParticleMotionDynamic::linearDragCoefficient),
            MolangValue.CODEC.optionalFieldOf("rotation_acceleration", MolangValue.FALSE_VALUE).forGetter(ParticleMotionDynamic::rotationAcceleration),
            MolangValue.CODEC.optionalFieldOf("rotation_drag_coefficient", MolangValue.FALSE_VALUE).forGetter(ParticleMotionDynamic::rotationDragCoefficient)
    ).apply(ins, ParticleMotionDynamic::new));

    public static class Data {
        float lastAge;
    }

    @Override
    public void onFrame(ParticleAccess particle) {
        Data motionDynamic = particle.blackboard().getOrCreate("motion_dynamic", Data.class, new Data());
        float age = particle.age();
        float deltaTime = age - motionDynamic.lastAge;
        MolangScope scope = particle.molangScope();

        Vector3f acceleration = linearAcceleration.eval(scope);
        float dragCoefficient = linearDragCoefficient.eval(scope);
        Vector3f velocity = particle.velocity();
        Vector3f dragForce = velocity.mul(-dragCoefficient, new Vector3f());
        acceleration.add(dragForce);
        velocity.add(acceleration.mul(deltaTime));
        particle.position().add(velocity.mul(deltaTime, new Vector3f()).div(16));

        float rotAcceleration = rotationAcceleration.eval(scope);
        float rotDragCoefficient = rotationDragCoefficient.eval(scope);
        float angularVelocity = particle.rotationRate();
        float totalRotAcceleration = rotAcceleration - rotDragCoefficient * angularVelocity;
        angularVelocity += totalRotAcceleration * deltaTime;
        particle.setRotationRate(angularVelocity);
        particle.setRotation(particle.rotation() + angularVelocity * deltaTime);
        motionDynamic.lastAge = age;
    }
}
