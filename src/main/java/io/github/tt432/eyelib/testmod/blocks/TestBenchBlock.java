package io.github.tt432.eyelib.testmod.blocks;

import com.mojang.serialization.MapCodec;
import io.github.tt432.eyelib.client.render.sections.RenderTypeExtension;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import io.github.tt432.eyelib.testmod.client.blocks.TestBenchBlockEntityRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;


public class TestBenchBlock extends BaseEntityBlock {
    public TestBenchBlock() {
        super(Properties.of().noCollission().noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(TestBenchBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestBenchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            EntityType<?> entityType = new EntityType<?>[] {EntityType.PIG, EntityType.SLIME, EntityType.ZOMBIE, EntityType.HUSK, EntityType.ALLAY, EntityType.SKELETON, EntityType.BOGGED, EntityType.COW, EntityType.CREEPER, EntityType.SNOW_GOLEM, EntityType.BLAZE, EntityType.SNIFFER, EntityType.DROWNED, EntityType.CHICKEN, EntityType.SQUID, EntityType.CAVE_SPIDER, EntityType.ENDERMAN}[level.random.nextInt(17)];
            DynamicChunkBuffers.markEntityChunkBuffer(entityType.create(level));
            ((TestBenchBlockEntity) level.getBlockEntity(pos)).setEntityType(entityType);
            Minecraft.getInstance().levelRenderer.allChanged();
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
