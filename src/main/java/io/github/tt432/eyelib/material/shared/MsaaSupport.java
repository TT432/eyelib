package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
/**
 * @author TT432
 */
public enum MsaaSupport implements PortStringRepresentable {
    Both, MSAA, NonMSAA;

    public static final Codec<MsaaSupport> CODEC = PortStringRepresentable.fromEnum(MsaaSupport::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}