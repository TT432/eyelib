package io.github.tt432.eyelib.bridge.client.manager;

import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
//?}

/** @author TT432 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} elif <26.1 {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class ManagerEventLifecycleHooks {
    private ManagerEventLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ManagerEventPublishBridge.install(new ForgeManagerEventPublisher());
    }
}
