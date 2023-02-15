package io.github.tt432.eyelib.common.bedrock.particle.component.particle.motion;

import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import net.minecraft.world.phys.Vec3;

/**
 * @author DustW
 */
public abstract class ParticleMotionComponent extends ParticleComponent {
    public abstract Vec3 getNewPos(MolangVariableScope scope, Vec3 pos);
}
