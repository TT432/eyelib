package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.attachment.dataattach.DataAttachmentType;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
//? if <1.20.6 {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
//?} else {
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
//?}
/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber
//?}
public class EyelibAttachableData {

    private static final ResourceLocation RENDER_DATA_ID =
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "render_data");
            //?} else {
            ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "render_data");
            //?}
    private static final ResourceLocation ITEM_IN_HAND_RENDER_DATA_ID =
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "item_in_hand_render_data");
            //?} else {
            ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "item_in_hand_render_data");
            //?}
    private static final ResourceLocation ENTITY_BEHAVIOR_DATA_ID =
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "entity_behavior_data");
            //?} else {
            ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "entity_behavior_data");
            //?}
    private static final ResourceLocation SYNCED_BEHAVIOR_STATE_ID =
            //? if <1.20.6 {
            new ResourceLocation(Eyelib.MOD_ID, "synced_behavior_state");
            //?} else {
            ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "synced_behavior_state");
            //?}

    // tt432: 所有 attachment 目前仅适用于 LivingEntity。
    //? if <1.20.6 {
    public static final RegistryObject<DataAttachmentType<RenderData<Object>>> RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(RENDER_DATA_ID.toString(), RenderData::new, RenderData.codec(), null));
    public static final RegistryObject<DataAttachmentType<ItemInHandRenderData>> ITEM_IN_HAND_RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ITEM_IN_HAND_RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ITEM_IN_HAND_RENDER_DATA_ID.toString(), ItemInHandRenderData::empty, ItemInHandRenderData.CODEC, null));
    public static final RegistryObject<DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID.toString(), EntityBehaviorData::new, null, EntityBehaviorData.STREAM_CODEC));
    public static final RegistryObject<DataAttachmentType<SyncedBehaviorState>> SYNCED_BEHAVIOR_STATE =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(SYNCED_BEHAVIOR_STATE_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(SYNCED_BEHAVIOR_STATE_ID.toString(), () -> SyncedBehaviorState.EMPTY, SyncedBehaviorState.CODEC, SyncedBehaviorState.STREAM_CODEC));
    //?} else {
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<RenderData<Object>>> RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(RENDER_DATA_ID.toString(), RenderData::new, RenderData.codec(), null));
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<ItemInHandRenderData>> ITEM_IN_HAND_RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ITEM_IN_HAND_RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ITEM_IN_HAND_RENDER_DATA_ID.toString(), ItemInHandRenderData::empty, ItemInHandRenderData.CODEC, null));
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID.toString(), EntityBehaviorData::new, null, EntityBehaviorData.STREAM_CODEC));
    public static final DeferredHolder<DataAttachmentType<?>, DataAttachmentType<SyncedBehaviorState>> SYNCED_BEHAVIOR_STATE =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(SYNCED_BEHAVIOR_STATE_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(SYNCED_BEHAVIOR_STATE_ID.toString(), () -> SyncedBehaviorState.EMPTY, SyncedBehaviorState.CODEC, SyncedBehaviorState.STREAM_CODEC));
    //?}
}
