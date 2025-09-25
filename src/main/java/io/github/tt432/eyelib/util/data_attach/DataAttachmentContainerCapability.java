package io.github.tt432.eyelib.util.data_attach;

import io.github.tt432.eyelib.Eyelib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class DataAttachmentContainerCapability {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "data_attachment");

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
            var outcome = event.getEntity().getCapability(INSTANCE);
            var original = event.getOriginal().getCapability(INSTANCE);
            var data = original.orElseThrow(IllegalAccessError::new).serializeNBT();
            outcome.orElseThrow(IllegalStateException::new).deserializeNBT(data);
        }

        @SubscribeEvent
        public static void onPlayerClone(LivingConversionEvent.Post event) {
            var outcome = event.getOutcome().getCapability(INSTANCE);
            var original = event.getEntity().getCapability(INSTANCE);
            var data = original.orElseThrow(IllegalAccessError::new).serializeNBT();
            outcome.orElseThrow(IllegalStateException::new).deserializeNBT(data);
        }
    }
}
