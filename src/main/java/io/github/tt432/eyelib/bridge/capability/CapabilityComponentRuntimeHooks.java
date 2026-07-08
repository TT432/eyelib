package io.github.tt432.eyelib.bridge.capability;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;
import io.github.tt432.eyelib.bridge.event.adapter.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.bridge.event.adapter.TextureChangedEvent;
import io.github.tt432.eyelib.animation.AnimationComponent;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//?}
/**
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class CapabilityComponentRuntimeHooks {
    private CapabilityComponentRuntimeHooks() {
    }

    @SubscribeEvent
    public static void onTextureChanged(TextureChangedEvent event) {
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.onTextureChanged();
    }

    @SubscribeEvent
    public static void onManagerEntryChanged(ManagerEntryChangedEvent event) {
        AnimationComponent.onManagerEntryChanged(event.getManagerName(), event.getEntryName());
    }
}

