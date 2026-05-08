package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * @author TT432
 */
public enum GLStates implements StringRepresentable {
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

    public static final Codec<GLStates> CODEC = StringRepresentable.fromEnum(GLStates::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
