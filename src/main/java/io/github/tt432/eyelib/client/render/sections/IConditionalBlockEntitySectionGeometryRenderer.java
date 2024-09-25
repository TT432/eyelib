package io.github.tt432.eyelib.client.render.sections;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface IConditionalBlockEntitySectionGeometryRenderer<T extends BlockEntity> extends IBlockEntitySectionGeometryRenderer<T> {
    boolean shouldRender(T blockEntity, BlockPos blockPos, BlockPos regionOrigin, Vec3 cameraPos);
}
