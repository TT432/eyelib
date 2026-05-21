package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public enum StencilDepthFailOp implements StringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilDepthFailOp> CODEC = StringRepresentable.fromEnum(StencilDepthFailOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}