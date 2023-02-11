package io.github.tt432.eyelib.common.bedrock.particle.component.particle.motion;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author DustW
 */
@ParticleComponentHolder("minecraft:particle_motion_dynamic")
public class Dynamic extends ParticleMotionComponent {
    /**
     * the linear acceleration applied to the particle, defaults to [0, 0, 0].
     * Units are blocks/sec/sec
     * An example would be gravity which is [0, -9.8, 0]
     * evaluated every frame
     */
    @SerializedName("linear_acceleration")
    Value3 linearAcceleration;

    /**
     * using the equation:
     * acceleration = -linear_drag_coefficient*velocity
     * where velocity is the current direction times speed
     * Think of this as air-drag.  The higher the value, the more drag
     * evaluated every frame
     */
    @SerializedName("linear_drag_coefficient")
    MolangValue linearDragCoefficient;

    /**
     * acceleration applies to the rotation speed of the particle
     * think of a disc spinning up or a smoke puff that starts rotating
     * but slows down over time
     * evaluated every frame
     * acceleration is in degrees/sec/sec
     */
    @SerializedName("rotation_acceleration")
    MolangValue rotationAcceleration;

    /**
     * drag applied to retard rotation
     * equation is rotation_acceleration += -rotation_rate*rotation_drag_coefficient
     * useful to slow a rotation, or to limit the rotation acceleration
     * Think of a disc that speeds up (acceleration)
     * but reaches a terminal speed (drag)
     * Another use is if you have a particle growing in size, having
     * the rotation slow down due to drag can add "weight" to the particle's
     * motion
     */
    @SerializedName("rotation_drag_coefficient")
    MolangValue rotationDragCoefficient;
}
