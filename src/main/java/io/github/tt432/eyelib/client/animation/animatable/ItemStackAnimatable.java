package io.github.tt432.eyelib.client.animation.animatable;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
public class ItemStackAnimatable implements Animatable<ItemStack> {
    ItemStack instance;

    public ItemStackAnimatable(ItemStack instance) {
        this.instance = instance;
    }

    @Override
    public @NotNull ItemStack instance() {
        return instance;
    }
}
