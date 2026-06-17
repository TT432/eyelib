package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public enum MsaaSupport implements PortStringRepresentable {
    Both, MSAA, NonMSAA;

    public static final Codec<MsaaSupport> CODEC = PortStringRepresentable.fromEnum(MsaaSupport::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}