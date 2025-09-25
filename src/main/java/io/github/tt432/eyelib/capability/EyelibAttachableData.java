package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.data_attach.DataAttachment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber
public class EyelibAttachableData {

    // <editor-fold desc="Capability ids">

    private static final ResourceLocation RENDER_DATA_ID = modLoc("render_data");
    private static final ResourceLocation ENTITY_STATISTICS_ID = modLoc("entity_statistics");
    private static final ResourceLocation EXTRA_ENTITY_UPDATE_ID = modLoc("extra_entity_update");
    private static final ResourceLocation EXTRA_ENTITY_DATA_ID = modLoc("extra_entity_data");
    private static final ResourceLocation ITEM_IN_HAND_RENDER_DATA_ID = modLoc("item_in_hand_render_data");
    private static final ResourceLocation ENTITY_BEHAVIOR_DATA_ID = modLoc("entity_behavior_data");

    // </editor-fold>

    // <editor-fold desc="Data Attachments">

    // tt432: All caps are only for LivingEntity.
    public static final DataAttachment<RenderData<Object>> RENDER_DATA = new DataAttachment<>(RENDER_DATA_ID, RenderData::new, RenderData.codec(), null);
    public static final DataAttachment<EntityStatistics> ENTITY_STATISTICS = new DataAttachment<>(ENTITY_STATISTICS_ID, EntityStatistics::empty, EntityStatistics.CODEC, EntityStatistics.STREAM_CODEC);
    public static final DataAttachment<ExtraEntityUpdateData> EXTRA_ENTITY_UPDATE = new DataAttachment<>(EXTRA_ENTITY_UPDATE_ID, ExtraEntityUpdateData::empty, ExtraEntityUpdateData.CODEC, null);
    public static final DataAttachment<ExtraEntityData> EXTRA_ENTITY_DATA = new DataAttachment<>(EXTRA_ENTITY_DATA_ID, ExtraEntityData::empty, ExtraEntityData.CODEC, null);
    public static final DataAttachment<ItemInHandRenderData> ITEM_IN_HAND_RENDER_DATA = new DataAttachment<>(ITEM_IN_HAND_RENDER_DATA_ID, ItemInHandRenderData::empty, ItemInHandRenderData.CODEC, null);
    public static final DataAttachment<EntityBehaviorData> ENTITY_BEHAVIOR_DATA = new DataAttachment<>(ENTITY_BEHAVIOR_DATA_ID, EntityBehaviorData::new, EntityBehaviorData.CODEC, EntityBehaviorData.STREAM_CODEC);

    // </editor-fold>

    private static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, path);
    }

    @SubscribeEvent
    public static void onRegister(RegisterCapabilitiesEvent event) {
        event.register(RenderData.class);
        event.register(EntityStatistics.class);
        event.register(ExtraEntityUpdateData.class);
        event.register(ExtraEntityData.class);
        event.register(ItemInHandRenderData.class);
        event.register(EntityBehaviorData.class);
    }

    @SubscribeEvent
    public static void onAttachEntity(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity livingEntity) {
            event.addCapability(RENDER_DATA.id(), RENDER_DATA.createDataAttachmentProvider());
            event.addCapability(ENTITY_STATISTICS.id(), ENTITY_STATISTICS.createDataAttachmentProvider());
            event.addCapability(EXTRA_ENTITY_UPDATE.id(), EXTRA_ENTITY_UPDATE.createDataAttachmentProvider());
            event.addCapability(EXTRA_ENTITY_DATA.id(), EXTRA_ENTITY_DATA.createDataAttachmentProvider());
            event.addCapability(ITEM_IN_HAND_RENDER_DATA.id(), ITEM_IN_HAND_RENDER_DATA.createDataAttachmentProvider());
            event.addCapability(ENTITY_BEHAVIOR_DATA.id(), ENTITY_BEHAVIOR_DATA.createDataAttachmentProvider());
        }
    }
}
