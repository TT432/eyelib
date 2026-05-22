package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum StencilFunc implements StringRepresentable {
    Always,
    Equal,
    NotEqual,
    Less,
    Greater,
    GreaterEqual,
    LessEqual;

    public static final Codec<StencilFunc> CODEC = StringRepresentable.fromEnum(StencilFunc::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}