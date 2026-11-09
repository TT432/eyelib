package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
//?} else {
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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

    /**
     * Detached entity currently rendered into a dev/smoke FBO scene
     * (26.1+ render-state 路径需要它定位实体；1.20.1 由事件直接携带实体，不使用本字段）。
     */
    public static @org.jspecify.annotations.Nullable LivingEntity sceneEntityOverride;

    //? if <26.1 {
    @SubscribeEvent
    public static <E extends LivingEntity, M extends EntityModel<E>> void onEvent(RenderLivingEvent.Pre<E, M> event) {
        LivingEntity entity = event.getEntity();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity,
                ((LivingEntityRendererAccessor) event.getRenderer()).callGetWhiteOverlayProgress(entity, event.getPartialTick()));
        var params = new RenderEntityParams(entity, event.getMultiBufferSource(), RenderSink.of(event.getMultiBufferSource()), event.getPoseStack(),
                                            event.getPackedLight(), event.getPartialTick(), overlay);
        if (RenderPorts.get().renderEntityPort().render(params)) {
            event.setCanceled(true);
            // 1.20.1 的原版栓绳在 super.render 返回后绘制，取消事件不会跳过它；
            // 标记本实体由 eyelib 接管，供 MobRendererLeashMixin 抑制原版绳。
            LeashSuppression.markHandled(entity);
        }
    }
    //?} else {
    @SubscribeEvent
    public static <T extends LivingEntity, S extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M extends EntityModel<? super S>> void onEvent(
            RenderLivingEvent.Pre<T, S, M> event) {
        var state = event.getRenderState();
        LivingEntity entity = sceneEntityOverride;
        if (entity == null) {
            // 优先取 extractEntity 记录的直接映射：CarryOn 等模组的假实体不在 level 里，
            // 下面的类型+坐标反查找不到它们（会退回 vanilla 模型）。
            entity = ExtractedEntityStates.get(state);
        }
        if (entity == null) {
            entity = findEntityByRenderState(state.entityType, state.x, state.y, state.z, event.getPartialTick());
        }
        if (entity == null) return;

        // 几何走 RenderSink 延迟提交（submitCustomGeometry，renderAllFeatures 阶段绘制）；
        // multiBufferSource 仅用于手持物等附属渲染。
        MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // 与原版 LivingEntityRenderer.submit 使用同一 overlay：受伤/死亡红闪由 hasRedOverlay 决定，
        // 白闪进度取渲染器覆写值（creeper 膨胀等）。原先固定 NO_OVERLAY 会让所有萌化生物
        // 丢失受伤红闪。
        var renderer = event.getRenderer();
        int overlay = LivingEntityRenderer.getOverlayCoords(state,
                ((LivingEntityRendererAccessor) renderer).callGetWhiteOverlayProgress(state));

        var params = new RenderEntityParams(entity, bufferSource, RenderSink.of(event.getSubmitNodeCollector()), event.getPoseStack(),
                                            state.lightCoords, event.getPartialTick(), overlay);
        boolean rendered = RenderPorts.get().renderEntityPort().render(params);

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
