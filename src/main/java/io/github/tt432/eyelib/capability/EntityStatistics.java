package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
@With
public record EntityStatistics(
        float distanceWalked
) {
    public static final Codec<EntityStatistics> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.fieldOf("distanceWalked").forGetter(o -> o.distanceWalked)
    ).apply(ins, EntityStatistics::new));

    public static final StreamCodec<EntityStatistics> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(EntityStatistics obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.FLOAT.encode(obj.distanceWalked, buf);
        }

        @Override
        public EntityStatistics decode(FriendlyByteBuf buf) {
            var distanceWalked = EyelibStreamCodecs.FLOAT.decode(buf);
            return new EntityStatistics(distanceWalked);
        }
    };

    public static EntityStatistics empty() {
        return new EntityStatistics(0);
    }
}
