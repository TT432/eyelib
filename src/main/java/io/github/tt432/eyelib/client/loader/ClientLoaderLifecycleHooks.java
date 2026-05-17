package io.github.tt432.eyelib.client.loader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientLoaderLifecycleHooks {
    private ClientLoaderLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(BrAnimationLoader.INSTANCE);
        event.registerReloadListener(BrAnimationControllerLoader.INSTANCE);
        event.registerReloadListener(BrParticleLoader.INSTANCE);
        event.registerReloadListener(BrMaterialLoader.INSTANCE);
        event.registerReloadListener(BrRenderControllerLoader.INSTANCE);
        event.registerReloadListener(BrClientEntityLoader.INSTANCE);
        event.registerReloadListener(BrAttachableLoader.INSTANCE);
        event.registerReloadListener(BrModelLoader.INSTANCE);
    }
}
