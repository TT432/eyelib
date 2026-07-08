package io.github.tt432.eyelib.bridge.particle;

import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Particle-owned pose initialization for emitters spawned from entity/locator context.
 */
/** @author TT432 */
public interface ParticleEmitterPoseInitializer {
    public static void initPose(
            BedrockParticleEmitter emitter,
            @Nullable Matrix4f locatorMatrix,
            @Nullable Entity attachedEntity
    ) {
        emitter.baseRotation().identity();
        //? if <26.1 {
        Vector3f cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();
        //?} else {
        Vector3f cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().position().toVector3f();
        //?}
        Matrix4f matrix = new Matrix4f().translate(cameraPosition, new Matrix4f());
        if (locatorMatrix != null) {
            matrix.mul(locatorMatrix);
        }

        if (emitter.space().position() || emitter.position().equals(0, 0, 0)) {
            if (locatorMatrix != null) {
                matrix.transformPosition(emitter.position().zero());
            } else if (attachedEntity != null) {
                emitter.position().set(attachedEntity.getX(), attachedEntity.getY(), attachedEntity.getZ());
            }
        }

        if (emitter.space().position() && emitter.space().rotation()) {
            if (locatorMatrix != null) {
                emitter.baseRotation().set(matrix);
            } else if (attachedEntity != null) {
                emitter.baseRotation().rotateZYX(new Vector3f(
                        (float) Math.toRadians(attachedEntity.getXRot()),
                        (float) Math.toRadians(attachedEntity.getYRot()),
                        0
                ));
            }
        }
    }
}
