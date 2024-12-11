package io.github.tt432.eyelib.client.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @author TT432
 */
public record BrSamplerState(
        int samplerIndex,
        String textureFilter
) {
    public static final Codec<BrSamplerState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("samplerIndex").forGetter(BrSamplerState::samplerIndex),
            Codec.STRING.fieldOf("textureFilter").forGetter(BrSamplerState::textureFilter)
    ).apply(ins, BrSamplerState::new));
}
