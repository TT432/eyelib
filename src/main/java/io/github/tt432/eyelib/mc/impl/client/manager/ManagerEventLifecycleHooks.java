package io.github.tt432.eyelib.mc.impl.client.manager;

import io.github.tt432.eyelib.client.manager.ManagerEventPublishBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ManagerEventLifecycleHooks {
    private ManagerEventLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ManagerEventPublishBridge.install(new ForgeManagerEventPublisher());
    }
}
