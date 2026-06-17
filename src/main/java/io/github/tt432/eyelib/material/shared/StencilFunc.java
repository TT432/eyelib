package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
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