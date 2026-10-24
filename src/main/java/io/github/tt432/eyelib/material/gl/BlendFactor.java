package io.github.tt432.eyelib.material.gl;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * Bedrock 混合因子（纯数据）。到 GL 常量的映射由 bridge 渲染层承担（ADR-0016）。
 *
 * @author TT432
 */
public enum BlendFactor implements PortStringRepresentable {
    DestColor,
    SourceColor,
    Zero,
    One,
    OneMinusDestColor,
    OneMinusSrcColor,
    SourceAlpha,
    DestAlpha,
    OneMinusSrcAlpha;

    public static final Codec<BlendFactor> CODEC = PortStringRepresentable.fromEnum(BlendFactor::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
