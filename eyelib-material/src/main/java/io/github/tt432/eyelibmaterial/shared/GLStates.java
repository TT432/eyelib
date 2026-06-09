package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum GLStates implements PortStringRepresentable {
    EnableAlphaToCoverage,
    Wireframe,
    Blending,
    DisableColorWrite,
    DisableAlphaWrite,
    DisableRgbWrite,
    DisableDepthTest,
    DisableDepthWrite,
    DisableCulling,
    InvertCulling,
    StencilWrite,
    EnableStencilTest;

    public static final Codec<GLStates> CODEC = PortStringRepresentable.fromEnum(GLStates::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}