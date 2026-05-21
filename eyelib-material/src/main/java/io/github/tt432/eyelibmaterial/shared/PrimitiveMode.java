package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public enum PrimitiveMode implements StringRepresentable {
    None, QuadList, TriangleList, TriangleStrip, LineList, LineStrip;

    public static final Codec<PrimitiveMode> CODEC = StringRepresentable.fromEnum(PrimitiveMode::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}