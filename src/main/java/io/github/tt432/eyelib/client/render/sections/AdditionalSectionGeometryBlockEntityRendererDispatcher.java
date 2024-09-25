package io.github.tt432.eyelib.client.render.sections;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.sections.cache.BakedModelsCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Argon4W
 */
public record AdditionalSectionGeometryBlockEntityRendererDispatcher(BlockPos regionOrigin) implements AddSectionGeometryEvent.AdditionalSectionRenderer {
    public static final Map<IBlockEntitySectionGeometryRenderer<?>, BakedModelsCache> CACHE = new ConcurrentHashMap<>();

    @Override
    public void render(@NotNull AddSectionGeometryEvent.SectionRenderingContext context) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int index = 0; index < 16 * 16 * 16; index ++) {
            int factor = index / 16;
            renderAt(cursor.set(regionOrigin.getX() + index % 16, regionOrigin.getY() + factor % 16, regionOrigin.getZ() + factor / 16), context);
        }
    }

    public void renderAt(BlockPos pos, AddSectionGeometryEvent.SectionRenderingContext context) {
        BlockEntity blockEntity = context.getRegion().getBlockEntity(pos);

        if (blockEntity == null) {
            return;
        }

        if (!(Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) instanceof IBlockEntitySectionGeometryRenderer<?> renderer)) {
            return;
        }

        if (renderer instanceof IConditionalBlockEntitySectionGeometryRenderer<?> conditional && !conditional.shouldRender(cast(blockEntity), pos, regionOrigin, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())) {
            return;
        }

        context.getPoseStack().pushPose();
        context.getPoseStack().translate(pos.getX() - regionOrigin.getX(), pos.getY() - regionOrigin.getY(), pos.getZ() - regionOrigin.getZ());

        try {
            renderer.renderSectionGeometry(cast(blockEntity), context, new PoseStack(), pos, regionOrigin, new LightAwareSectionGeometryRenderContext(context, CACHE.computeIfAbsent(renderer, renderer1 -> new BakedModelsCache()), pos, regionOrigin));
        } catch (ClassCastException ignored) {

        }

        context.getPoseStack().popPose();
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> T cast(BlockEntity o) {
        return (T) o;
    }
}
