package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
@With
public record ExtraEntityData(
        boolean facing_target_to_range_attack,
        boolean is_avoiding_mobs,
        boolean is_grazing,
        boolean is_avoid,
        boolean is_dig
) {
    public static final ExtraEntityData EMPTY = new ExtraEntityData(
            false,
            false,
            false,
            false,
            false
    );

    public static final Codec<ExtraEntityData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.fieldOf("facing_target_to_range_attack").forGetter(ExtraEntityData::facing_target_to_range_attack),
            Codec.BOOL.fieldOf("is_avoiding_mobs").forGetter(ExtraEntityData::is_avoiding_mobs),
            Codec.BOOL.fieldOf("is_grazing").forGetter(ExtraEntityData::is_grazing),
            Codec.BOOL.fieldOf("is_avoid").forGetter(ExtraEntityData::is_avoid),
            Codec.BOOL.fieldOf("is_dig").forGetter(ExtraEntityData::is_dig)
    ).apply(ins, ExtraEntityData::new));

    public static final StreamCodec<ExtraEntityData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ExtraEntityData obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.BOOL.encode(obj.facing_target_to_range_attack, buf);
            EyelibStreamCodecs.BOOL.encode(obj.is_avoiding_mobs, buf);
            EyelibStreamCodecs.BOOL.encode(obj.is_grazing, buf);
            EyelibStreamCodecs.BOOL.encode(obj.is_avoid, buf);
            EyelibStreamCodecs.BOOL.encode(obj.is_dig, buf);
        }

        @Override
        public ExtraEntityData decode(FriendlyByteBuf buf) {
            var facingTargetToRangeAttack = EyelibStreamCodecs.BOOL.decode(buf);
            var isAvoidingMobs = EyelibStreamCodecs.BOOL.decode(buf);
            var isGrazing = EyelibStreamCodecs.BOOL.decode(buf);
            var isAvoid = EyelibStreamCodecs.BOOL.decode(buf);
            var isDig = EyelibStreamCodecs.BOOL.decode(buf);
            return new ExtraEntityData(facingTargetToRangeAttack, isAvoidingMobs, isGrazing, isAvoid, isDig);
        }
    };

    public static ExtraEntityData empty() {
        return EMPTY;
    }
}
