package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelibattachment.dataattach.DataAttachmentType;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibbehavior.EntityBehaviorData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber

/** @author TT432 */
@NullMarked
public class EyelibAttachableData {

    private static final ResourceLocation RENDER_DATA_ID = new ResourceLocation(Eyelib.MOD_ID, "render_data");
    private static final ResourceLocation ITEM_IN_HAND_RENDER_DATA_ID = new ResourceLocation(Eyelib.MOD_ID, "item_in_hand_render_data");
    private static final ResourceLocation ENTITY_BEHAVIOR_DATA_ID = new ResourceLocation(Eyelib.MOD_ID, "entity_behavior_data");

    // tt432: All attachments are only for LivingEntity now.
    public static final RegistryObject<DataAttachmentType<RenderData<Object>>> RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(RENDER_DATA_ID.toString(), RenderData::new, RenderData.codec(), null));
    public static final RegistryObject<DataAttachmentType<ItemInHandRenderData>> ITEM_IN_HAND_RENDER_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ITEM_IN_HAND_RENDER_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ITEM_IN_HAND_RENDER_DATA_ID.toString(), ItemInHandRenderData::empty, ItemInHandRenderData.CODEC, null));
    public static final RegistryObject<DataAttachmentType<EntityBehaviorData>> ENTITY_BEHAVIOR_DATA =
            DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(ENTITY_BEHAVIOR_DATA_ID.getPath(),
                                                                 () -> new DataAttachmentType<>(ENTITY_BEHAVIOR_DATA_ID.toString(), EntityBehaviorData::new, EntityBehaviorData.CODEC, EntityBehaviorData.STREAM_CODEC));
}
