package io.github.tt432.eyelib.material.gl.stencil;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * 模板测试失败时的操作（纯数据）。
 *
 * @author TT432
 */
public enum StencilFailOp implements PortStringRepresentable {
    /** 保留缓冲区原有值 */
    Keep,
    /** 使用参考值与掩码的按位与结果替换缓冲区值 */
    Replace;

    public static final Codec<StencilFailOp> CODEC = PortStringRepresentable.fromEnum(StencilFailOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
