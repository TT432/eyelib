package io.github.tt432.eyelib.client.gui.tooltip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.ExtraFaceData;

import java.util.Optional;

/**
 * @author TT432
 */
public record ReplaceTooltipData(
        ResourceLocation texture,
        Color color
) {
    public static final Codec<ReplaceTooltipData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            net.minecraft.resources.ResourceLocation.CODEC.optionalFieldOf("texture", ResourceLocations.EMPTY)
                    .forGetter(o -> o.texture),
            Color.CODEC.optionalFieldOf("color", Color.EMPTY).forGetter(o -> o.color)
    ).apply(ins, ReplaceTooltipData::new));

    public static final StreamCodec<ByteBuf, ReplaceTooltipData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ReplaceTooltipData::texture,
            Color.STREAM_CODEC,
            ReplaceTooltipData::color,
            ReplaceTooltipData::new
    );

    public record Color(
            Optional<Integer> backgroundTop,
            Optional<Integer> backgroundBottom,
            Optional<Integer> borderTop,
            Optional<Integer> borderBottom
    ) {
        public static final Color EMPTY = new Color(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        public static final Codec<Color> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                ExtraFaceData.COLOR.optionalFieldOf("background_top").forGetter(o -> o.backgroundTop),
                ExtraFaceData.COLOR.optionalFieldOf("background_bottom").forGetter(o -> o.backgroundBottom),
                ExtraFaceData.COLOR.optionalFieldOf("border_top").forGetter(o -> o.borderTop),
                ExtraFaceData.COLOR.optionalFieldOf("border_bottom").forGetter(o -> o.borderBottom)
        ).apply(ins, Color::new));

        public static final StreamCodec<ByteBuf, Color> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.INT),
                Color::backgroundTop,
                ByteBufCodecs.optional(ByteBufCodecs.INT),
                Color::backgroundBottom,
                ByteBufCodecs.optional(ByteBufCodecs.INT),
                Color::borderTop,
                ByteBufCodecs.optional(ByteBufCodecs.INT),
                Color::borderBottom,
                Color::new
        );

        public boolean isEmpty() {
            return this == EMPTY;
        }
    }
}
