package io.github.tt432.eyelib.behavior.component;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * @author TT432
 */
public record EmptyComponent() implements Component {
    public static final EmptyComponent INSTANCE = new EmptyComponent();

    public static final Codec<EmptyComponent> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "empty";
    }
}