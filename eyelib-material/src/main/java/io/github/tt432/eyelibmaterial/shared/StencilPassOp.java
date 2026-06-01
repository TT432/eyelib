package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum StencilPassOp implements StringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilPassOp> CODEC = StringRepresentable.fromEnum(StencilPassOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}