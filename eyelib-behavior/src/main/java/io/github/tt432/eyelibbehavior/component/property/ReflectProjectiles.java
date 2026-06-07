package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:reflect_projectiles — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ReflectProjectiles() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final ReflectProjectiles INSTANCE = new ReflectProjectiles();

    public static final Codec<ReflectProjectiles> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "reflect_projectiles";
    }
}
