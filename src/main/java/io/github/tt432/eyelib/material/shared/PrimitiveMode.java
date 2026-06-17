package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum PrimitiveMode implements PortStringRepresentable {
    None, QuadList, TriangleList, TriangleStrip, LineList, LineStrip;

    public static final Codec<PrimitiveMode> CODEC = PortStringRepresentable.fromEnum(PrimitiveMode::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}