package io.github.tt432.eyelib.capability.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.gui.tooltip.ReplaceTooltipData;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;

/**
 * @author TT432
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemTooltipReplaceData {
    public static final Codec<ItemTooltipReplaceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ReplaceTooltipData.CODEC.fieldOf("data").forGetter(o -> o.data)
    ).apply(ins, ItemTooltipReplaceData::new));

    public static final StreamCodec<ByteBuf, ItemTooltipReplaceData> STREAM_CODEC = StreamCodec.composite(
            ReplaceTooltipData.STREAM_CODEC,
            ItemTooltipReplaceData::getData,
            ItemTooltipReplaceData::new
    );

    ReplaceTooltipData data;
}
