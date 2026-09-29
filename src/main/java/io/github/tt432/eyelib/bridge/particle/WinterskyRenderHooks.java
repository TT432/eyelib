package io.github.tt432.eyelib.bridge.particle;

import io.github.tt432.eyelib.bridge.particle.adapter.WinterskyParticleRenderer;
import io.github.tt432.eyelib.bridge.particle.adapter.WinterskySceneManager;

//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

/**
 * wintersky 粒子预览的 Forge 事件接线（ADR-0035 §3.2，模式照搬 {@link ParticleRenderHooks}）：
 * RenderTick START 驱动固定步长 tick（D4）→ AFTER_ENTITIES 渲染 → LoggingOut 清空。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class WinterskyRenderHooks {
    private WinterskyRenderHooks() {
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
    //?} else {
    public static void onRenderTick(RenderFrameEvent.Pre event) {
    //?}
        WinterskySceneManager.onRenderTickStart();
    }

    //? if <26.1 {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        new WinterskyParticleRenderer(event.getPoseStack()).render();
    }
    //?}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WinterskySceneManager.clear();
    }
}
