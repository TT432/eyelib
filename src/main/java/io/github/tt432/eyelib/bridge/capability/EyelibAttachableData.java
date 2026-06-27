package io.github.tt432.eyelib.bridge.capability;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.bridge.Eyelib;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
//? if <1.20.6 {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
//?} else {
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
//?}

import java.util.function.Supplier;

/**
 * Bridge 侧数据附属类型注册中心。
 * domain 类型（EntityBehaviorData/SyncedBehaviorState）直接声明；
 * application 类型（RenderData/ItemInHandRenderData）由 {@link io.github.tt432.eyelib.capability.AttachableDataTypes} 调用 {@link #register} 注册。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public class EyelibAttachableData {

    //? if <26.1 {
    private static final ResourceLocation ENTITY_BEHAVIOR_DATA_ID =
    //?} else {
    private static final Identifier ENTITY_BEHAVIOR_DATA_ID =
    //?}
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "entity_behavior_data");
            //?} else {
            Identifier.fromNamespaceAndPath(Eyelib.MOD_ID, "entity_behavior_data");

            //?}
    //? if <26.1 {
    private static final ResourceLocation SYNCED_BEHAVIOR_STATE_ID =
    //?} else {
    private static final Identifier SYNCED_BEHAVIOR_STATE_ID =
    //?}
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "synced_behavior_state");
            //?} else {
            Identifier.fromNamespaceAndPath(Eyelib.MOD_ID, "synced_behavior_state");

            //?}

    //? if <1.20.6 {
    public static final RegistryObject<DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID.toString(), EntityBehaviorData::new, null, EntityBehaviorData.STREAM_CODEC));
    public static final RegistryObject<DataAttachmentType<SyncedBehaviorState>> SYNCED_BEHAVIOR_STATE =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(SYNCED_BEHAVIOR_STATE_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(SYNCED_BEHAVIOR_STATE_ID.toString(), () -> SyncedBehaviorState.EMPTY, SyncedBehaviorState.CODEC, SyncedBehaviorState.STREAM_CODEC));
    //?} else {
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID.toString(), EntityBehaviorData::new, null, EntityBehaviorData.STREAM_CODEC));
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<SyncedBehaviorState>> SYNCED_BEHAVIOR_STATE =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(SYNCED_BEHAVIOR_STATE_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(SYNCED_BEHAVIOR_STATE_ID.toString(), () -> SyncedBehaviorState.EMPTY, SyncedBehaviorState.CODEC, SyncedBehaviorState.STREAM_CODEC));
    //?}

    public static DataAttachmentType<SyncedBehaviorState> syncedBehaviorState() {
        return SYNCED_BEHAVIOR_STATE.get();
    }

    public static DataAttachmentType<EntityBehaviorData> entityBehaviorData() {
        return ENTITY_BEHAVIOR_DATA.get();
    }

    public static <T> Supplier<DataAttachmentType<T>> register(
            String name, Supplier<T> factory, Supplier<Codec<T>> codecSupplier) {
        return DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(name,
                () -> new DataAttachmentType<>("eyelib:" + name, factory, codecSupplier.get(), null));
    }
}
