package io.github.tt432.eyelib.client.manager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.NullMarked;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)

/** @author TT432 */
@NullMarked
public final class ManagerEventLifecycleHooks {
    private ManagerEventLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ManagerEventPublishBridge.install(new ForgeManagerEventPublisher());
    }
}
