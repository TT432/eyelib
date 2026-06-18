package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;

/**
 * Bedrock 顶点字段语义枚举，仅用于序列化标识。
 * 到具体 MC {@code VertexFormatElement} 的映射由 bridge 层完成。
 *
 * @author TT432
 */
public enum VertexFormatElementEnum implements PortStringRepresentable {
    Position,
    Normal,
    UV0,
    UV1,
    Color,
    BoneId0;

    public static final Codec<VertexFormatElementEnum> CODEC =
            PortStringRepresentable.fromEnum(VertexFormatElementEnum::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
