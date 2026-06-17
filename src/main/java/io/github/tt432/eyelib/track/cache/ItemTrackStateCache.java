package io.github.tt432.eyelibtrack.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.function.LongFunction;

/**
 * 按追踪 ID 缓存任意数据的通用容器。
 *
 * @param <T> 缓存的数据类型
 * @author TT432
 */
public class ItemTrackStateCache<T> {
    private final Long2ObjectMap<T> store = new Long2ObjectOpenHashMap<>();
    private final LongFunction<T> factory;

    public ItemTrackStateCache(LongFunction<T> factory) {
        this.factory = factory;
    }

    /**
     * 获取或创建指定 ID 对应的缓存数据。
     */
    public T getOrCreate(long id) {
        return store.computeIfAbsent(id, factory);
    }

    /**
     * 移除指定 ID 的缓存数据。
     */
    public void remove(long id) {
        store.remove(id);
    }
}