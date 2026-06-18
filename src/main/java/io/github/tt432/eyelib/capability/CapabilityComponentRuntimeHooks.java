package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.event.TextureChangedEvent;
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
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
//?}
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
