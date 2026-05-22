package io.github.tt432.eyelibtrack.api;

import net.minecraft.world.item.ItemStack;

/**
 * 需要 per-ItemStack 追踪的物品应实现此接口。
 *
 * @author TT432
 */
/** @author TT432 */
public interface TrackableItem {

    /**
     * 是否需要为每个 ItemStack 实例维护独立的追踪状态。
     */
    default boolean needsItemTracking() {
        return false;
    }

    /**
     * 检查指定 ItemStack 是否需要追踪。
     * 默认委托给 {@link #needsItemTracking()}。
     */
    static boolean isTrackable(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof TrackableItem ti
                && ti.needsItemTracking();
    }
}
