package io.github.tt432.eyelib.client.animation.animatable;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
public interface Animatable<T> {
    /**
     * Do not assume that EMPTY is based on any subclass implementation.
     * EMPTY is only used for placeholder where non-null is required.
     */
    Animatable<?> EMPTY = new ItemStackAnimatable(ItemStack.EMPTY);

    @NotNull
    T instance();
}
