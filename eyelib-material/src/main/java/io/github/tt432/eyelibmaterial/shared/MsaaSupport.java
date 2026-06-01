package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum MsaaSupport implements StringRepresentable {
    Both, MSAA, NonMSAA;

    public static final Codec<MsaaSupport> CODEC = StringRepresentable.fromEnum(MsaaSupport::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}