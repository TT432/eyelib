package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum StencilFailOp implements PortStringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilFailOp> CODEC = PortStringRepresentable.fromEnum(StencilFailOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}