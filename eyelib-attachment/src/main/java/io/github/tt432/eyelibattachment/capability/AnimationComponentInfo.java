package io.github.tt432.eyelibattachment.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record AnimationComponentInfo(
        Map<String, String> animations,
        Map<String, MolangValue> animate
) {
    public static final Codec<AnimationComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("animations").forGetter(AnimationComponentInfo::animations),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).fieldOf("animate").forGetter(AnimationComponentInfo::animate)
    ).apply(ins, AnimationComponentInfo::new));

    private static final StreamCodec<MolangValue> MOLANG_VALUE_STREAM_CODEC = EyelibStreamCodecs.fromCodec(MolangValue.CODEC);

    public static final StreamCodec<AnimationComponentInfo> STREAM_CODEC = new StreamCodec<>() {
        private final StreamCodec<Map<String, String>> animationsCodec = EyelibStreamCodecs.map(HashMap::new, EyelibStreamCodecs.STRING, EyelibStreamCodecs.STRING);
        private final StreamCodec<Map<String, MolangValue>> animateCodec = EyelibStreamCodecs.map(HashMap::new, EyelibStreamCodecs.STRING, MOLANG_VALUE_STREAM_CODEC);

        @Override
        public void encode(AnimationComponentInfo obj, net.minecraft.network.FriendlyByteBuf buf) {
            animationsCodec.encode(obj.animations(), buf);
            animateCodec.encode(obj.animate(), buf);
        }

        @Override
        public AnimationComponentInfo decode(net.minecraft.network.FriendlyByteBuf buf) {
            var animations = animationsCodec.decode(buf);
            var animate = animateCodec.decode(buf);
            return new AnimationComponentInfo(animations, animate);
        }
    };
}