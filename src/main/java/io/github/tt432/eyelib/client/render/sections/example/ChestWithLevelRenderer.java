package io.github.tt432.eyelib.client.render.sections.example;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.sections.IConditionalBlockEntitySectionGeometryRenderer;
import io.github.tt432.eyelib.client.render.sections.ISectionGeometryRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;

/**
 * 使用例：
 * <pre>{@code
 * @Mixin(ChestRenderer.class)
 * public abstract class ChestRendererMixin<T extends BlockEntity & LidBlockEntity> implements ChestWithLevelRenderer<T> {
 * }
 * }</pre>
 *
 * <pre>{@code
 * @Mixin(ChestBlockEntity.class)
 * public class ChestBlockEntityMixin implements IChestBlockEntityExtension {
 *     @Unique
 *     private boolean eyelib$lastShouldRender;
 *
 *     @Override
 *     public boolean getLastShouldRender() {
 *         return eyelib$lastShouldRender;
 *     }
 *
 *     @Override
 *     public void setLastShouldRender(boolean v) {
 *         eyelib$lastShouldRender = v;
 *     }
 * }
 * }</pre>
 *
 * @author TT432
 */
public interface ChestWithLevelRenderer<T extends BlockEntity & LidBlockEntity> extends BlockEntityRenderer<T>, IConditionalBlockEntitySectionGeometryRenderer<T> {
    @Override
    default void renderSectionGeometry(T blockEntity, AddSectionGeometryEvent.SectionRenderingContext context, PoseStack poseStack, BlockPos pos, BlockPos regionOrigin, ISectionGeometryRenderContext renderAndCacheContext) {
        ChestWithLevelRenderers.render(context, poseStack, pos, regionOrigin, renderAndCacheContext);
    }

    @Override
    default boolean shouldRender(T blockEntity, BlockPos blockPos, BlockPos regionOrigin, Vec3 cameraPos) {
        return !shouldRender(blockEntity, cameraPos);
    }

    @Override
    default boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        boolean result = blockEntity.getOpenNess(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) > 0;

        if (!(blockEntity instanceof IChestBlockEntityExtension extension)) {
            return true;
        }

        if (extension.getLastShouldRender() == result) {
            return result;
        }

        extension.setLastShouldRender(result);
        BlockState blockState = blockEntity.getBlockState();
        Level level = blockEntity.getLevel();

        if (level == null) {
            return result;
        }

        RenderSystem.recordRenderCall(() -> level.sendBlockUpdated(blockEntity.getBlockPos(), blockState, blockState, UPDATE_CLIENTS));
        return result;
    }
}
