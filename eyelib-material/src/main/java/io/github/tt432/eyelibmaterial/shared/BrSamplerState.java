package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/**
 * Pure data record for a single sampler state entry.
 * <p>
 * No GL/LWJGL/MC dependencies — suitable for platform-free serialization.
 *
 * @author TT432
 */
public record BrSamplerState(
        int samplerIndex,
        TextureFilter textureFilter,
        TextureWrap textureWrap
) {
    public static final Codec<BrSamplerState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("samplerIndex").forGetter(BrSamplerState::samplerIndex),
            TextureFilter.CODEC.fieldOf("textureFilter").forGetter(BrSamplerState::textureFilter),
            TextureWrap.CODEC.fieldOf("textureWrap").forGetter(BrSamplerState::textureWrap)
    ).apply(ins, BrSamplerState::new));

    public enum TextureFilter implements StringRepresentable {
        Point,
        Bilinear,
        Trilinear,
        MipMapBilinear,
        TexelAA,
        PCF;

        public static final Codec<TextureFilter> CODEC = StringRepresentable.fromEnum(TextureFilter::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum TextureWrap implements StringRepresentable {
        Repeat,
        Clamp;

        public static final Codec<TextureWrap> CODEC = StringRepresentable.fromEnum(TextureWrap::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}
