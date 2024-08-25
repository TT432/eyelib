package io.github.tt432.eyelib.client.render.level.example;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.render.level.WithLevelRenderer;
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
public interface ChestWithLevelRenderer<T extends BlockEntity & LidBlockEntity> extends BlockEntityRenderer<T>, WithLevelRenderer<T> {
    @Override
    default void render(BlockPos pos, BlockPos origin, AddSectionGeometryEvent.SectionRenderingContext context) {
        ChestWithLevelRenderers.render(pos, origin, context);
    }

    @Override
    default boolean needRender(T object, Vec3 cameraPos) {
        return !shouldRender(object, cameraPos);
    }

    boolean getLastShouldRender();

    void setLastShouldRender(boolean v);

    @Override
    default boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        boolean result = blockEntity.getOpenNess(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) > 0;

        if (getLastShouldRender() != result) {
            setLastShouldRender(result);

            BlockState blockState = blockEntity.getBlockState();
            Level level = blockEntity.getLevel();

            if (level != null)
                RenderSystem.recordRenderCall(() -> level.sendBlockUpdated(blockEntity.getBlockPos(), blockState, blockState, UPDATE_CLIENTS));
        }

        return result;
    }
}
