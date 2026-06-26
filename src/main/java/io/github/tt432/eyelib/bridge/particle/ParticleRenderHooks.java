package io.github.tt432.eyelib.bridge.particle;

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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

/**
 * Forge event wiring for the particle module client integration layer.
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
//?}
/** @author TT432 */
public final class ParticleRenderHooks {
    private ParticleRenderHooks() {
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
        ParticleRuntimeBridge.RENDER_MANAGER.onRenderTickStart();
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
    //?} else {
    public static void onClientTick(ClientTickEvent.Pre event) {
    //?}
        ParticleRuntimeBridge.RENDER_MANAGER.onClientTickStart();
    }

    //? if <26.1 {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        ParticleRuntimeBridge.RENDER_MANAGER.renderAfterEntities(new BedrockParticleRenderer(event.getPoseStack()));
    }
    //?}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleRuntimeBridge.RENDER_MANAGER.clear();
    }
}
