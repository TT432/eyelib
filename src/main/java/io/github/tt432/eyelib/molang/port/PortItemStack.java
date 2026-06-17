package io.github.tt432.eyelib.molang.port;

import org.jspecify.annotations.NullMarked;

/**
 * 物品栈抽象。
 *
 * @author TT432
 */
@NullMarked
public interface PortItemStack {
    int getCount();
    int getMaxStackSize();
}
