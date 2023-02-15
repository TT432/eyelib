package io.github.tt432.eyelib.common.bedrock.particle.component.particle.motion;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import net.minecraft.world.phys.Vec3;

/**
 * @author DustW
 */
@ParticleComponentHolder("minecraft:particle_motion_parametric")
public class Parametric extends ParticleMotionComponent {
    /**
     * directly set the position relative to the emitter.
     * E.g. a spiral might be:
     * "relative_position": ["Math.cos(Params.LifeTime)", 1.0,
     * "Math.sin(Params.Lifetime)"]
     * defaults to [0, 0, 0]
     * evaluated every frame
     */
    @SerializedName("relative_position")
    Value3 offset;

    /**
     * directly set the 3d direction of the particle
     * doesn't affect direction if not specified
     * evaluated every frame
     */
    Value3 direction;

    /**
     * directly set the rotation of the particle
     * evaluated every frame
     */
    MolangValue rotation;

    // TODO 实现 direction 和 rotation
    @Override
    public Vec3 getNewPos(MolangVariableScope scope, Vec3 pos) {
        return offset.evaluate(scope);
    }
}
