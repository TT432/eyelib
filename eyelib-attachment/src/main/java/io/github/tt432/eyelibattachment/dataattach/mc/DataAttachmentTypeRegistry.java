package io.github.tt432.eyelibattachment.dataattach.mc;

import io.github.tt432.eyelibattachment.capability.EntityStatistics;
import io.github.tt432.eyelibattachment.capability.ExtraEntityData;
import io.github.tt432.eyelibattachment.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelibattachment.dataattach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataAttachmentTypeRegistry {
    private static final ResourceKey<Registry<DataAttachmentType<?>>> DATA_ATTACHMENTS_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("eyelib", "data_attachments"));

    public static final DeferredRegister<DataAttachmentType<?>> DATA_ATTACHMENTS =
            DeferredRegister.create(DATA_ATTACHMENTS_KEY, "eyelib");

    public static final Supplier<IForgeRegistry<DataAttachmentType<?>>> REGISTRY =
            DATA_ATTACHMENTS.makeRegistry(RegistryBuilder::new);

    public static final Supplier<DataAttachmentType<EntityStatistics>> ENTITY_STATISTICS =
            DATA_ATTACHMENTS.register("entity_statistics",
                    () -> new DataAttachmentType<>("eyelib:entity_statistics",
                            EntityStatistics::empty, EntityStatistics.CODEC, EntityStatistics.STREAM_CODEC));

    public static final Supplier<DataAttachmentType<ExtraEntityUpdateData>> EXTRA_ENTITY_UPDATE =
            DATA_ATTACHMENTS.register("extra_entity_update",
                    () -> new DataAttachmentType<>("eyelib:extra_entity_update",
                            ExtraEntityUpdateData::empty, ExtraEntityUpdateData.CODEC, ExtraEntityUpdateData.STREAM_CODEC));

    public static final Supplier<DataAttachmentType<ExtraEntityData>> EXTRA_ENTITY_DATA =
            DATA_ATTACHMENTS.register("extra_entity_data",
                    () -> new DataAttachmentType<>("eyelib:extra_entity_data",
                            ExtraEntityData::empty, ExtraEntityData.CODEC, ExtraEntityData.STREAM_CODEC));

    public static DataAttachmentType<?> getById(String id) {
        var optional = REGISTRY.get().getValue(new ResourceLocation(id));
        if (optional != null) {
            return optional;
        } else {
            throw new IllegalStateException("Unknown attachment type: " + id);
        }
    }
}