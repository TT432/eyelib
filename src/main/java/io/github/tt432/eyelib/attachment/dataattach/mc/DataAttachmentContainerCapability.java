package io.github.tt432.eyelib.attachment.dataattach.mc;

import io.github.tt432.eyelib.attachment.dataattach.IDataAttachmentContainer;
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

/**
 * 数据附属容器的 Minecraft Forge Capability 系统集成。
 *
 * @author TT432
 */
public class DataAttachmentContainerCapability {
    public static final ResourceLocation ID = new ResourceLocation("eyelibattachment", "data_attachments");

    public static final Capability<IDataAttachmentContainer> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Mod.EventBusSubscriber(modid = "eyelib", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusHandlers {
        @SubscribeEvent
        public static void onRegister(RegisterCapabilitiesEvent event) {
            event.register(IDataAttachmentContainer.class);
        }
    }

    @Mod.EventBusSubscriber(modid = "eyelib")
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