package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
/**
 * @author TT432
 */
public enum StencilPassOp implements PortStringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilPassOp> CODEC = PortStringRepresentable.fromEnum(StencilPassOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}