package io.github.tt432.eyelib.material.gl;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * 深度比较函数（纯数据）。到 GL 常量的映射由 bridge 渲染层承担（ADR-0016）。
 *
 * @author TT432
 */
public enum DepthFunc implements PortStringRepresentable {
    Always,
    Equal,
    NotEqual,
    Less,
    Greater,
    GreaterEqual,
    LessEqual;

    public static final Codec<DepthFunc> CODEC = PortStringRepresentable.fromEnum(DepthFunc::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
