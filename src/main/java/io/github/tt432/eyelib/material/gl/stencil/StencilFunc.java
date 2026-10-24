package io.github.tt432.eyelib.material.gl.stencil;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * 模板比较函数（纯数据）。
 *
 * @author TT432
 */
public enum StencilFunc implements PortStringRepresentable {
    /** 总是通过模板测试 */
    Always,
    /** 当参考值等于缓冲区值时通过测试 */
    Equal,
    /** 当参考值不等于缓冲区值时通过测试 */
    NotEqual,
    /** 当参考值小于缓冲区值时通过测试 */
    Less,
    /** 当参考值大于缓冲区值时通过测试 */
    Greater,
    /** 当参考值大于等于缓冲区值时通过测试 */
    GreaterEqual,
    /** 当参考值小于等于缓冲区值时通过测试 */
    LessEqual;

    public static final Codec<StencilFunc> CODEC = PortStringRepresentable.fromEnum(StencilFunc::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
