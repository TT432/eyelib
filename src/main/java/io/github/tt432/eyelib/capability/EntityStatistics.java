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
public record EntityStatistics(
        float preDistanceWalked,
        float distanceWalked
) {
    public static final Codec<EntityStatistics> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.fieldOf("preDistanceWalked").forGetter(EntityStatistics::preDistanceWalked),
            Codec.FLOAT.fieldOf("distanceWalked").forGetter(EntityStatistics::distanceWalked)
    ).apply(ins, EntityStatistics::new));

    public static final StreamCodec<ByteBuf, EntityStatistics> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            EntityStatistics::preDistanceWalked,
            ByteBufCodecs.FLOAT,
            EntityStatistics::distanceWalked,
            EntityStatistics::new
    );

    public static EntityStatistics empty() {
        return new EntityStatistics(0, 0);
    }
}
