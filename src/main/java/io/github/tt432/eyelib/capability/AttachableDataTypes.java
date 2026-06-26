package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.bridge.capability.EyelibAttachableData;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;

import java.util.function.Supplier;

/**
 * 应用层声明的数据附属类型注册。
 * 通过 {@link EyelibAttachableData#register} 向 bridge 注册，避免 bridge 反向引用 application 数据类型。
 *
 * @author TT432
 */
public final class AttachableDataTypes {
    private AttachableDataTypes() {}

    public static final Supplier<DataAttachmentType<RenderData<Object>>> RENDER_DATA =
            EyelibAttachableData.register("render_data", RenderData::new, () -> RenderData.codec());

    public static final Supplier<DataAttachmentType<ItemInHandRenderData>> ITEM_IN_HAND_RENDER_DATA =
            EyelibAttachableData.register("item_in_hand_render_data", ItemInHandRenderData::empty, () -> ItemInHandRenderData.CODEC);
}
