package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.bridge.client.ClientTickHandler;
import io.github.tt432.eyelib.particle.runtime.bedrock.ParticleRuntimeEnvironment;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import java.util.Optional;

/**
 * 基于 Minecraft 客户端环境的 {@link ParticleRuntimeEnvironment} 实现。
 *
 * @author TT432
 */
public record MinecraftParticleRuntimeEnvironment(Level level) implements ParticleRuntimeEnvironment {
    @Override
    public int ticks() {
        return ClientTickHandler.getTick();
    }

    @Override
    public float partialTick() {
        //? if <1.20.6 {
        return Minecraft.getInstance().timer.partialTick;
        //?} else {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        //?}
    }

    @Override
    public Optional<EmitterParticleComponent.EmitterAccess.Bounds> entityBounds() {
        Entity entity = Minecraft.getInstance().player;
        if (entity == null) {
            return Optional.empty();
        }
        AABB bounds = entity.getBoundingBox();
        return Optional.of(new EmitterParticleComponent.EmitterAccess.Bounds(
                new Vector3f((float) bounds.getCenter().x, (float) bounds.getCenter().y, (float) bounds.getCenter().z),
                new Vector3f((float) bounds.getXsize() / 2F, (float) bounds.getYsize() / 2F, (float) bounds.getZsize() / 2F)
        ));
    }

    @Override
    public Optional<String> blockAtPosition(Vector3f position) {
        return Optional.of(BuiltInRegistries.BLOCK.getKey(level.getBlockState(BlockPos.containing(
                position.x,
                position.y,
                position.z
        )).getBlock()).toString());
    }
}
