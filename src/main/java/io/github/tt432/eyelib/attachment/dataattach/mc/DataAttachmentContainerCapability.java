package io.github.tt432.eyelib.attachment.dataattach.mc;

import io.github.tt432.eyelib.attachment.dataattach.IDataAttachmentContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
//? if <1.20.6 {
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;
//?}

/**
 * 数据附属容器的 Minecraft 平台集成。
 *
 * @author TT432
 */
//? if <1.20.6 {
public class DataAttachmentContainerCapability {
    public static final ResourceLocation ID = new ResourceLocation("eyelib", "data_attachments");

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
//?} else {
public class DataAttachmentContainerCapability {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "eyelib");

    public static final Supplier<AttachmentType<McDataAttachmentContainer>> ATTACHMENT =
            ATTACHMENT_TYPES.register("data_attachments", () ->
                    AttachmentType.<CompoundTag, McDataAttachmentContainer>serializable(McDataAttachmentContainer::new)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
//?}
