package io.github.tt432.eyelib.material.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL46.GL_MAX_TEXTURE_MAX_ANISOTROPY;
import static org.lwjgl.opengl.GL46.GL_TEXTURE_MAX_ANISOTROPY;

/**
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

    public enum TextureFilter implements PortStringRepresentable {
        Point(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }),
        Bilinear(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }),
        Trilinear(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }),
        MipMapBilinear(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }),
        TexelAA(() -> {
            // 启用各向异性过滤（需要先查询硬件支持的最大级别）
            float[] maxAnisotropy = new float[1];
            glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy[0]);
        }),
        PCF(() -> {
            // TODO: PCF 未实现
        });

        public final Runnable onUse;

        TextureFilter(Runnable onUse) {
            this.onUse = onUse;
        }

        public static final Codec<TextureFilter> CODEC = PortStringRepresentable.fromEnum(TextureFilter::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum TextureWrap implements PortStringRepresentable {
        Repeat(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        }),

        Clamp(() -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        });

        public static final Codec<TextureWrap> CODEC = PortStringRepresentable.fromEnum(TextureWrap::values);
        public final Runnable onUse;

        TextureWrap(Runnable onUse) {
            this.onUse = onUse;
        }

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}