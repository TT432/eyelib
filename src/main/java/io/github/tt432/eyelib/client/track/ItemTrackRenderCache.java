package io.github.tt432.eyelib.client.track;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.track.api.ItemTrackApi;
import io.github.tt432.eyelib.track.cache.ItemTrackStateCache;
import net.minecraft.world.item.ItemStack;

/**
 * 将追踪 ID 映射到 {@link RenderData RenderData&lt;ItemStack&gt;} 的缓存绑定。
 *
 * @author TT432
 */
public final class ItemTrackRenderCache {
    private static final ItemTrackStateCache<RenderData<ItemStack>> CACHE = new ItemTrackStateCache<>(
            id -> {
                RenderData<ItemStack> rd = new RenderData<>();
                rd.init(ItemStack.EMPTY);
                return rd;
            }
    );

    private ItemTrackRenderCache() {
    }

    /**
     * 获取或创建指定 ItemStack 对应的 RenderData。
     * 如果 ItemStack 尚未绑定 owner，则绑定之。
     */
    public static RenderData<ItemStack> getOrCreateRenderData(ItemStack stack) {
        long id = ItemTrackApi.getId(stack);
        RenderData<ItemStack> rd = CACHE.getOrCreate(id);

        rd.ensureOwner(stack);

        return rd;
    }

    /**
     * 移除指定 ID 的缓存数据。
     */
    public static void invalidate(long id) {
        CACHE.remove(id);
    }
}
