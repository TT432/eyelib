package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
//?} else {
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
//?}
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
//?}

/**
 * RenderLivingEvent.Pre 适配器，将渲染实体事件翻译为版本无关参数后转发到 application 层 Port。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class RenderLivingEventAdapter {
    private RenderLivingEventAdapter() {
    }

    //? if <26.1 {
    @SubscribeEvent
    public static <E extends LivingEntity, M extends EntityModel<E>> void onEvent(RenderLivingEvent.Pre<E, M> event) {
        LivingEntity entity = event.getEntity();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity,
                ((LivingEntityRendererAccessor) event.getRenderer()).callGetWhiteOverlayProgress(entity, event.getPartialTick()));

        var params = new RenderEntityParams(entity, event.getMultiBufferSource(), event.getPoseStack(),
                                            event.getPackedLight(), event.getPartialTick(), overlay);
        if (EntityRenderPorts.renderEntityPort.render(params)) {
            event.setCanceled(true);
        }
    }
    //?} else {
    @SubscribeEvent
    public static <T extends LivingEntity, S extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M extends EntityModel<? super S>> void onEvent(
            RenderLivingEvent.Pre<T, S, M> event) {
        var state = event.getRenderState();
        LivingEntity entity = findEntityByRenderState(state.entityType, state.x, state.y, state.z, event.getPartialTick());
        if (entity == null) return;

        com.mojang.blaze3d.vertex.ByteBufferBuilder byteBufferBuilder = new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBufferBuilder);

        var params = new RenderEntityParams(entity, bufferSource, event.getPoseStack(),
                                            state.lightCoords, event.getPartialTick(), OverlayTexture.NO_OVERLAY);
        boolean rendered = EntityRenderPorts.renderEntityPort.render(params);

        bufferSource.endBatch();
        byteBufferBuilder.close();
        if (rendered) event.setCanceled(true);
    }

    @Nullable
    private static LivingEntity findEntityByRenderState(
            net.minecraft.world.entity.EntityType<?> entityType,
            double targetX,
            double targetY,
            double targetZ,
            float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;

        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (entity.getType() != entityType) continue;

            double renderX = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double renderY = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double renderZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            if (Math.abs(renderX - targetX) < 0.5
                    && Math.abs(renderY - targetY) < 0.5
                    && Math.abs(renderZ - targetZ) < 0.5) {
                return livingEntity;
            }
        }

        return null;
    }
    //?}
}
