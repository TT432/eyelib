//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.event;

import io.github.tt432.eyelib.debug.rendercount.RenderTypeDrawStats;
import net.minecraft.client.Minecraft;
//? if <1.20.6 {
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
//?}

/**
 * 1.20.1 / 1.21.1 的帧边界与 F3 文本注入。
 *
 * <p>F3 文本走 {@code CustomizeGuiOverlayEvent.DebugText}（两个版本都在 DebugScreenOverlay
 * 渲染左列之前 post），因此不需要 mixin GUI。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib")
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public final class RenderTypeDrawDebugHandler {
    private RenderTypeDrawDebugHandler() {
    }

    //? if <1.20.6 {
    /** 1.20.1 的帧起点：RenderTickEvent.START 早于 GameRenderer.render。 */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            RenderTypeDrawStats.beginFrame();
        }
    }
    //?} else {
    /** 1.21.1 的帧起点：RenderFrameEvent.Pre 早于 GameRenderer.render。 */
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        RenderTypeDrawStats.beginFrame();
    }
    //?}

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!isDebugOverlayVisible()) {
            return;
        }
        event.getLeft().addAll(RenderTypeDrawStats.snapshot().toDebugLines());
    }

    //? if <1.20.6 {
    private static boolean isDebugOverlayVisible() {
        return Minecraft.getInstance().options.renderDebug;
    }
    //?} else {
    private static boolean isDebugOverlayVisible() {
        return Minecraft.getInstance().getDebugOverlay().showDebugScreen();
    }
    //?}
}
//?}
