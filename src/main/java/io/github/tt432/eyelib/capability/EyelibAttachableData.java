package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber
public class EyelibAttachableData {

    public static final ResourceKey<Registry<DataAttachmentType<?>>> DATA_ATTACHMENTS_KEY = ResourceKey.createRegistryKey(modLoc("data_attachments"));

    public static final DeferredRegister<DataAttachmentType<?>> DATA_ATTACHMENTS = DeferredRegister.create(DATA_ATTACHMENTS_KEY, Eyelib.MOD_ID);
    public static final Supplier<IForgeRegistry<DataAttachmentType<?>>> REGISTRY = DATA_ATTACHMENTS.makeRegistry(RegistryBuilder::new);

    // <editor-fold desc="Capability ids">

    private static final ResourceLocation RENDER_DATA_ID = modLoc("render_data");
    private static final ResourceLocation ENTITY_STATISTICS_ID = modLoc("entity_statistics");
    private static final ResourceLocation EXTRA_ENTITY_UPDATE_ID = modLoc("extra_entity_update");
    private static final ResourceLocation EXTRA_ENTITY_DATA_ID = modLoc("extra_entity_data");
    private static final ResourceLocation ITEM_IN_HAND_RENDER_DATA_ID = modLoc("item_in_hand_render_data");
    private static final ResourceLocation ENTITY_BEHAVIOR_DATA_ID = modLoc("entity_behavior_data");

    // </editor-fold>

    // <editor-fold desc="Data Attachments">

    // tt432: All attachments are only for LivingEntity now.
    public static final RegistryObject<DataAttachmentType<RenderData<Object>>> RENDER_DATA = DATA_ATTACHMENTS.register(RENDER_DATA_ID.getPath(), () -> new DataAttachmentType<>(RENDER_DATA_ID, RenderData::new, RenderData.codec(), null));
    public static final RegistryObject<DataAttachmentType<EntityStatistics>> ENTITY_STATISTICS = DATA_ATTACHMENTS.register(ENTITY_STATISTICS_ID.getPath(), () -> new DataAttachmentType<>(ENTITY_STATISTICS_ID, EntityStatistics::empty, EntityStatistics.CODEC, EntityStatistics.STREAM_CODEC));
    public static final RegistryObject<DataAttachmentType<ExtraEntityUpdateData>> EXTRA_ENTITY_UPDATE = DATA_ATTACHMENTS.register(EXTRA_ENTITY_UPDATE_ID.getPath(), () -> new DataAttachmentType<>(EXTRA_ENTITY_UPDATE_ID, ExtraEntityUpdateData::empty, ExtraEntityUpdateData.CODEC, ExtraEntityUpdateData.STREAM_CODEC));
    public static final RegistryObject<DataAttachmentType<ExtraEntityData>> EXTRA_ENTITY_DATA = DATA_ATTACHMENTS.register(EXTRA_ENTITY_DATA_ID.getPath(), () -> new DataAttachmentType<>(EXTRA_ENTITY_DATA_ID, ExtraEntityData::empty, ExtraEntityData.CODEC, ExtraEntityData.STREAM_CODEC));
    public static final RegistryObject<DataAttachmentType<ItemInHandRenderData>> ITEM_IN_HAND_RENDER_DATA = DATA_ATTACHMENTS.register(ITEM_IN_HAND_RENDER_DATA_ID.getPath(), () -> new DataAttachmentType<>(ITEM_IN_HAND_RENDER_DATA_ID, ItemInHandRenderData::empty, ItemInHandRenderData.CODEC, null));
    public static final RegistryObject<DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA = DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(), () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID, EntityBehaviorData::new, EntityBehaviorData.CODEC, EntityBehaviorData.STREAM_CODEC));

    // </editor-fold>

    private static ResourceLocation modLoc(String path) {
        return new ResourceLocation(Eyelib.MOD_ID, path);
    }

    public static DataAttachmentType<?> getById(ResourceLocation id) {
        var optional = REGISTRY.get().getValue(id);
        if (optional != null) {
            return optional;
        } else {
            throw new IllegalStateException("Unknown attachment type: " + id);
        }
    }
}
