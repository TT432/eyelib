package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum StencilFailOp implements StringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilFailOp> CODEC = StringRepresentable.fromEnum(StencilFailOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}