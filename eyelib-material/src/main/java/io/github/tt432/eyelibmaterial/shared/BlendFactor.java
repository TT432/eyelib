package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
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