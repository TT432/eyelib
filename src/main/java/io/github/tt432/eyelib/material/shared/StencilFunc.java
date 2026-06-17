package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
/**
 * @author TT432
 */
public enum StencilFunc implements PortStringRepresentable {
    Always,
    Equal,
    NotEqual,
    Less,
    Greater,
    GreaterEqual,
    LessEqual;

    public static final Codec<StencilFunc> CODEC = PortStringRepresentable.fromEnum(StencilFunc::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}