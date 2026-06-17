package io.github.tt432.eyelib.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import org.jspecify.annotations.NullMarked;

/**
 * 由服务端权威计算、同步给客户端的轻量行为状态数据。
 *
 * @author TT432
 */
@NullMarked
public record SyncedBehaviorState(
        int variant,
        float scale,
        int markVariant
) {
    public static final SyncedBehaviorState EMPTY = new SyncedBehaviorState(0, 1.0f, 0);

    public static final Codec<SyncedBehaviorState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("variant", 0).forGetter(SyncedBehaviorState::variant),
            Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(SyncedBehaviorState::scale),
            Codec.INT.optionalFieldOf("mark_variant", 0).forGetter(SyncedBehaviorState::markVariant)
    ).apply(ins, SyncedBehaviorState::new));

    public static final StreamCodec<SyncedBehaviorState> STREAM_CODEC = EyelibStreamCodecs.fromCodec(CODEC);
}
