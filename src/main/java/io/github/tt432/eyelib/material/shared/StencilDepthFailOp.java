package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
/**
 * @author TT432
 */
public enum StencilDepthFailOp implements PortStringRepresentable {
    Keep,
    Replace;

    public static final Codec<StencilDepthFailOp> CODEC = PortStringRepresentable.fromEnum(StencilDepthFailOp::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}