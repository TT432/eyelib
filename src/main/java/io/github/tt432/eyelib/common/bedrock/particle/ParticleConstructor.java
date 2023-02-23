package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.common.bedrock.particle.component.particle.*;
import io.github.tt432.eyelib.common.bedrock.particle.component.particle.motion.ParticleMotionComponent;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.ParticleFile;
import lombok.Builder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author DustW
 */
@Builder
public class ParticleConstructor {
    @NotNull
    ParticleLifetimeExpression lifetime;

    @NotNull
    ParticleInitialSpeed speed;
    @Nullable
    ParticleInitialSpin spin;
    @Nullable
    ParticleKillPlane killPlane;

    @NotNull
    ParticleAppearanceBillboard billboard;
    @Nullable
    ParticleAppearanceLighting lighting;
    @Nullable
    ParticleAppearanceTinting tinting;

    @Nullable
    ParticleMotionComponent motionComponent;

    @Nullable
    ParticleExpireIfInBlocks inBlocks;
    @Nullable
    ParticleExpireIfNotInBlocks notInBlocks;
    @Nullable
    ParticleMotionCollision collision;

    public static ParticleConstructor from(ParticleFile particleFile) {
        var components = particleFile.getEffect().getComponents();

        return ParticleConstructor.builder()
                .lifetime(components.getByClass(ParticleLifetimeExpression.class))
                .speed(components.getByClass(ParticleInitialSpeed.class))
                .spin(components.getByClass(ParticleInitialSpin.class))
                .killPlane(components.getByClass(ParticleKillPlane.class))
                .billboard(components.getByClass(ParticleAppearanceBillboard.class))
                .lighting(components.getByClass(ParticleAppearanceLighting.class))
                .tinting(components.getByClass(ParticleAppearanceTinting.class))
                .motionComponent(components.getByClass(ParticleMotionComponent.class))
                .inBlocks(components.getByClass(ParticleExpireIfInBlocks.class))
                .notInBlocks(components.getByClass(ParticleExpireIfNotInBlocks.class))
                .collision(components.getByClass(ParticleMotionCollision.class))
                .build();
    }

    public ParticleInstance construct(Level level, Vec3 pos) {
        return ParticleInstance.builder()
                .level(level)
                .worldPos(pos)
                .lifetime(lifetime)
                .speed(speed)
                .spin(spin)
                .killPlane(killPlane)
                .billboard(billboard)
                .lighting(lighting)
                .tinting(tinting)
                .motionComponent(motionComponent)
                .inBlocks(inBlocks)
                .notInBlocks(notInBlocks)
                .collision(collision)
                .build();
    }
}
