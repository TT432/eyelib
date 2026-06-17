package io.github.tt432.eyelib.attachment.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
@With
public record ExtraEntityUpdateData(
        int targetId,
        double lastHurtX,
        double lastHurtY,
        double lastHurtZ,
        float speed
) {
    public static final Codec<ExtraEntityUpdateData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("targetId").forGetter(ExtraEntityUpdateData::targetId),
            Codec.DOUBLE.fieldOf("lastHurtX").forGetter(ExtraEntityUpdateData::lastHurtX),
            Codec.DOUBLE.fieldOf("lastHurtY").forGetter(ExtraEntityUpdateData::lastHurtY),
            Codec.DOUBLE.fieldOf("lastHurtZ").forGetter(ExtraEntityUpdateData::lastHurtZ),
            Codec.FLOAT.fieldOf("speed").forGetter(ExtraEntityUpdateData::speed)
    ).apply(ins, ExtraEntityUpdateData::new));

    public static final StreamCodec<ExtraEntityUpdateData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ExtraEntityUpdateData obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.targetId, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtX, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtY, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtZ, buf);
            EyelibStreamCodecs.FLOAT.encode(obj.speed, buf);
        }

        @Override
        public ExtraEntityUpdateData decode(FriendlyByteBuf buf) {
            var targetId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var lastHurtX = EyelibStreamCodecs.DOUBLE.decode(buf);
            var lastHurtY = EyelibStreamCodecs.DOUBLE.decode(buf);
            var lastHurtZ = EyelibStreamCodecs.DOUBLE.decode(buf);
            var speed = EyelibStreamCodecs.FLOAT.decode(buf);
            return new ExtraEntityUpdateData(targetId, lastHurtX, lastHurtY, lastHurtZ, speed);
        }
    };

    public static ExtraEntityUpdateData empty() {
        return new ExtraEntityUpdateData(-1, 0, 0, 0, 0);
    }
}