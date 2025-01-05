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
        int mark_variant,
        boolean facing_target_to_range_attack,
        boolean is_avoiding_mobs,
        boolean is_grazing,
        boolean is_avoid
) {
    public static final ExtraEntityData EMPTY = new ExtraEntityData(
            -1,
            -1,
            false,
            false,
            false,
            false
    );

    public static final Codec<ExtraEntityData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("variant").forGetter(ExtraEntityData::variant),
            Codec.INT.fieldOf("mark_variant").forGetter(ExtraEntityData::mark_variant),
            Codec.BOOL.fieldOf("facing_target_to_range_attack").forGetter(ExtraEntityData::facing_target_to_range_attack),
            Codec.BOOL.fieldOf("is_avoiding_mobs").forGetter(ExtraEntityData::is_avoiding_mobs),
            Codec.BOOL.fieldOf("is_grazing").forGetter(ExtraEntityData::is_grazing),
            Codec.BOOL.fieldOf("is_avoid").forGetter(ExtraEntityData::is_avoid)
    ).apply(ins, ExtraEntityData::new));

    public static final StreamCodec<ByteBuf, ExtraEntityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ExtraEntityData::variant,
            ByteBufCodecs.VAR_INT,
            ExtraEntityData::mark_variant,
            ByteBufCodecs.BOOL,
            ExtraEntityData::facing_target_to_range_attack,
            ByteBufCodecs.BOOL,
            ExtraEntityData::is_avoiding_mobs,
            ByteBufCodecs.BOOL,
            ExtraEntityData::is_grazing,
            ByteBufCodecs.BOOL,
            ExtraEntityData::is_avoid,
            ExtraEntityData::new
    );

    public static ExtraEntityData empty() {
        return EMPTY;
    }
}
