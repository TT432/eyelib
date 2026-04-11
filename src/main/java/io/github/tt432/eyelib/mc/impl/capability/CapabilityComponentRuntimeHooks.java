package io.github.tt432.eyelib.mc.impl.capability;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapabilityComponentRuntimeHooks {
    private CapabilityComponentRuntimeHooks() {
    }

    @SubscribeEvent
    public static void onTextureChanged(TextureChangedEvent event) {
        RenderControllerComponent.onTextureStateChanged();
    }

    @SubscribeEvent
    public static void onManagerEntryChanged(ManagerEntryChangedEvent event) {
        AnimationComponent.onManagerEntryChanged(event.getManagerName(), event.getEntryName());
    }
}
