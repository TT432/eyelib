package io.github.tt432.eyelib.mc.impl.data_attach;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.data_attach.IDataAttachmentContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class DataAttachmentContainerCapability {
    public static final ResourceLocation ID = new ResourceLocation(Eyelib.MOD_ID, "data_attachments");

    public static final Capability<IDataAttachmentContainer> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusHandlers {
        @SubscribeEvent
        public static void onRegister(RegisterCapabilitiesEvent event) {
            event.register(IDataAttachmentContainer.class);
        }
    }

    @Mod.EventBusSubscriber
    public static class GameBusHandlers {
        @SubscribeEvent
        public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
            event.addCapability(ID, new DataAttachmentContainerProvider());
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (!event.isWasDeath()) {
                var original = event.getOriginal();
                original.reviveCaps();
                var originalCaps = original.getCapability(INSTANCE);
                var source = (McDataAttachmentContainer) originalCaps.orElseThrow(IllegalAccessError::new);
                var data = source.serializeNBT();
                original.invalidateCaps();

                var outcome = event.getEntity().getCapability(INSTANCE);
                var target = (McDataAttachmentContainer) outcome.orElseThrow(IllegalStateException::new);
                target.deserializeNBT(data);
            }
        }
    }
}
