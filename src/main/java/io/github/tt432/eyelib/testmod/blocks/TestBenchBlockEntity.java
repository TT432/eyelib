package io.github.tt432.eyelib.testmod.blocks;

import io.github.tt432.eyelib.testmod.EyelibTestMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TestBenchBlockEntity extends BlockEntity {
    private EntityType<?> entityType;

    public TestBenchBlockEntity(BlockPos pos, BlockState blockState) {
        super(EyelibTestMod.TEST_BENCH_BLOCK_ENTITY_TYPE.get(), pos, blockState);
        entityType = EntityType.BOGGED;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }
}
