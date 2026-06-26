package io.github.tt432.eyelib.bridge.attachment.dataattach.mc;

import io.github.tt432.eyelib.util.entitydata.EntityStatistics;
import io.github.tt432.eyelib.util.entitydata.ExtraEntityData;
import io.github.tt432.eyelib.util.entitydata.ExtraEntityUpdateData;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
//? if >=26.1
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
//? if <1.20.6 {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
//?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
//?}

import java.util.function.Supplier;

/**
 * 数据附属类型的注册中心。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataAttachmentTypeRegistry {
    private static final ResourceKey<Registry<DataAttachmentType<?>>> DATA_ATTACHMENTS_KEY =
            //? if <1.20.6 {
            ResourceKey.createRegistryKey(new ResourceLocation("eyelib", "data_attachments"));
            //?} else {
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("eyelib", "data_attachments"));
            //?}

    public static final DeferredRegister<DataAttachmentType<?>> DATA_ATTACHMENTS =
            DeferredRegister.create(DATA_ATTACHMENTS_KEY, "eyelib");

    //? if <1.20.6 {
    public static final Supplier<IForgeRegistry<DataAttachmentType<?>>> REGISTRY =
            DATA_ATTACHMENTS.makeRegistry(RegistryBuilder::new);
    //?} else {
    public static final Registry<DataAttachmentType<?>> REGISTRY =
            DATA_ATTACHMENTS.makeRegistry(builder -> {});
    //?}

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
        //? if <1.20.6 {
        var value = REGISTRY.get().getValue(new ResourceLocation(id));
        //?} elif <26.1 {
        var value = REGISTRY.get(ResourceLocation.parse(id));
        //?} else {
        var value = REGISTRY.get(Identifier.parse(id)).map(r -> r.value()).orElse(null);
        //?}
        if (value != null) {
            return value;
        } else {
            throw new IllegalStateException("Unknown attachment type: " + id);
        }
    }
}
