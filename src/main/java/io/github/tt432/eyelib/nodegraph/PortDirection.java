package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;

/** 端口方向。 */
public enum PortDirection implements PortStringRepresentable {
    IN,
    OUT;

    public static final Codec<PortDirection> CODEC = PortStringRepresentable.fromEnum(PortDirection::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
