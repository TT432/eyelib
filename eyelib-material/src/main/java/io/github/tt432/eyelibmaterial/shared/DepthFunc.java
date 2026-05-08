package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * @author TT432
 */
public enum DepthFunc implements StringRepresentable {
    Always,
    Equal,
    NotEqual,
    Less,
    Greater,
    GreaterEqual,
    LessEqual;

    public static final Codec<DepthFunc> CODEC = StringRepresentable.fromEnum(DepthFunc::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
