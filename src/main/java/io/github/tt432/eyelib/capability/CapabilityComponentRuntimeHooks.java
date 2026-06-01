package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import io.github.tt432.eyelibanimation.AnimationComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NullMarked;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)

/** @author TT432 */
@NullMarked
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
