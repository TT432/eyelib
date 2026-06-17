package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum BlendFactor implements PortStringRepresentable {
    DestColor,
    SourceColor,
    Zero,
    One,
    OneMinusDestColor,
    OneMinusSrcColor,
    SourceAlpha,
    DestAlpha,
    OneMinusSrcAlpha;

    public static final Codec<BlendFactor> CODEC = PortStringRepresentable.fromEnum(BlendFactor::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}