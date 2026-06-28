package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.util.event.api.RenderStageRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

/**
 * 渲染阶段事件适配器：订阅 Forge {@code RenderLevelStageEvent}，翻译版本差异后
 * 通过 {@link RenderStageRegistries#renderStage()} 分发给 {@code @OnRenderStage} 标记的方法。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class RenderStageEventAdapter {
    private RenderStageEventAdapter() {
    }

    //? if <26.1 {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        //? if <1.20.6 {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTick = event.getPartialTick();
        //?} else {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        //?}

        RenderStageRegistries.renderStage().dispatch(partialTick, position.x, position.y, position.z);
    }
    //?} else {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        RenderStageRegistries.renderStage().dispatch(partialTick, position.x, position.y, position.z);

        var sharedBuffer = new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(sharedBuffer);
        RenderPorts.get().renderBufferPort.renderEntities(
                partialTick, position.x, position.y, position.z, event.getPoseStack(), bufferSource);
        try { bufferSource.endBatch(); } catch (Throwable ignored) {}
        sharedBuffer.close();
    }
    //?}
}
