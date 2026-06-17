package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
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