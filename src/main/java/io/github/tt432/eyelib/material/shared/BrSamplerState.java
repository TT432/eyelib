package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * 单个采样器状态条目的纯数据记录。
 *
 * @author TT432
 */
@NullMarked
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

    public enum TextureFilter implements PortStringRepresentable {
        Point,
        Bilinear,
        Trilinear,
        MipMapBilinear,
        TexelAA,
        PCF;

        public static final Codec<TextureFilter> CODEC = PortStringRepresentable.fromEnum(TextureFilter::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum TextureWrap implements PortStringRepresentable {
        Repeat,
        Clamp;

        public static final Codec<TextureWrap> CODEC = PortStringRepresentable.fromEnum(TextureWrap::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}