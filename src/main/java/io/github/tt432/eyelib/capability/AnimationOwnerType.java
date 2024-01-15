package io.github.tt432.eyelib.capability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/**
 * TODO 为 server -> client 的同步做准备
 *
 * @author TT432
 */
public enum AnimationOwnerType {
    ENTITY(obj -> ((Entity) obj).getId()),
    ITEM_STACK(obj -> 0/* TODO */),
    UNKNOWN(obj -> -1)
    // TODO and more...
    ;

    public final Function<Object, Integer> id;

    AnimationOwnerType(Function<Object, Integer> id) {
        this.id = id;
    }

    public static AnimationOwnerType of(Object o) {
        return o instanceof Entity ? ENTITY
                : o instanceof ItemStack ? ITEM_STACK
                : UNKNOWN;
    }
}
