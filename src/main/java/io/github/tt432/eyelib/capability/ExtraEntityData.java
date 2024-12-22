package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.With;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * @author TT432
 */
@With
public record ExtraEntityData(
        int variant,
        int mark_variant
) {
    public static final ExtraEntityData EMPTY = new ExtraEntityData(-1, -1);

    public static ExtraEntityData empty() {
        return EMPTY;
    }

    public static final StreamCodec<ByteBuf, ExtraEntityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            o -> o.variant,
            ByteBufCodecs.VAR_INT,
            o -> o.mark_variant,
            ExtraEntityData::new
    );

    public static final Codec<ExtraEntityData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("variant").forGetter(ExtraEntityData::variant),
            Codec.INT.fieldOf("mark_variant").forGetter(ExtraEntityData::mark_variant)
    ).apply(ins, ExtraEntityData::new));
}
