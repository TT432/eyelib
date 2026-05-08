package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * @author TT432
 */
public enum BlendFactor implements StringRepresentable {
    DestColor,
    SourceColor,
    Zero,
    One,
    OneMinusDestColor,
    OneMinusSrcColor,
    SourceAlpha,
    DestAlpha,
    OneMinusSrcAlpha;

    public static final Codec<BlendFactor> CODEC = StringRepresentable.fromEnum(BlendFactor::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
