package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.bridge.client.ClientFrameTimePort;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
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
        return ClientTickPort.getTick();
    }

    @Override
    public float partialTick() {
        return ClientFrameTimePort.getGameTimeDeltaPartialTick();
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

    @Override
    public java.util.List<CollisionBox> collisionBoxes(Vector3f center, float radius) {
        int minX = net.minecraft.util.Mth.floor(center.x - radius);
        int minY = net.minecraft.util.Mth.floor(center.y - radius);
        int minZ = net.minecraft.util.Mth.floor(center.z - radius);
        int maxX = net.minecraft.util.Mth.floor(center.x + radius);
        int maxY = net.minecraft.util.Mth.floor(center.y + radius);
        int maxZ = net.minecraft.util.Mth.floor(center.z + radius);
        java.util.List<CollisionBox> result = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    var shape = state.getCollisionShape(level, pos,
                            net.minecraft.world.phys.shapes.CollisionContext.empty());
                    if (shape.isEmpty()) continue;
                    for (AABB aabb : shape.toAabbs()) {
                        result.add(new CollisionBox(
                                aabb.minX + x, aabb.minY + y, aabb.minZ + z,
                                aabb.maxX + x, aabb.maxY + y, aabb.maxZ + z));
                    }
                }
            }
        }
        return result;
    }
}
