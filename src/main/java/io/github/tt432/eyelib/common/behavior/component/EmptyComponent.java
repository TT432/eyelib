package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;

/**
 * @author TT432
 */
public record EmptyComponent() implements Component {
    public static final EmptyComponent INSTANCE = new EmptyComponent();

    public static final Codec<EmptyComponent> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "empty";
    }
}
