package io.github.tt432.eyelib.client.render.level;

import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

/**
 * @author TT432
 */
@EventBusSubscriber(Dist.CLIENT)
public class AddSectionGeometryEventListener {
    @SuppressWarnings("unchecked")
    static <T> T cast(Object o) {
        return (T) o;
    }

    @AllArgsConstructor
    public static final class WithLevelRendererAdditionalSectionRenderer
            implements AddSectionGeometryEvent.AdditionalSectionRenderer {
        private final AddSectionGeometryEvent event;

        @Override
        public void render(AddSectionGeometryEvent.SectionRenderingContext context) {
            BlockAndTintGetter region = context.getRegion();
            BlockPos posSource = event.getSectionOrigin();
            BlockPos posTarget = posSource.offset(15, 15, 15);

            for (BlockPos pos : BlockPos.betweenClosed(posSource, posTarget)) {
                BlockEntity blockEntity = region.getBlockEntity(pos);

                if (blockEntity == null) continue;

                BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
                BlockEntityRenderer<BlockEntity> renderer = dispatcher.getRenderer(blockEntity);
                Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

                if (renderer instanceof WithLevelRenderer<?> r && r.needRender(cast(blockEntity), cameraPos)) {
                    r.render(pos, event.getSectionOrigin(), context);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEvent(AddSectionGeometryEvent event) {
        event.addRenderer(new WithLevelRendererAdditionalSectionRenderer(event));
    }
}
