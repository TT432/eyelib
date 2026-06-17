package io.github.tt432.eyelib.track.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.track.EyelibTrack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 服务端 SavedData：单调递增的追踪 ID 计数器，数据持久化在存档中。
 *
 * @author TT432
 */
public final class ItemStackIdCache extends SavedData {
    private static final String DATA_KEY = EyelibTrack.TRACK_ID_KEY;

    private long lastId;

    private ItemStackIdCache() {
        this.lastId = 0;
    }

    private static ItemStackIdCache load(CompoundTag tag) {
        ItemStackIdCache cache = new ItemStackIdCache();
        cache.lastId = tag.getLong("lastId");
        return cache;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("lastId", lastId);
        return tag;
    }

    /**
     * 分配一个新的全局唯一追踪 ID。
     */
    public static long getFreeId(ServerLevel level) {
        ItemStackIdCache cache = level.getServer().overworld()
                .getDataStorage()
                .computeIfAbsent(ItemStackIdCache::load, ItemStackIdCache::new, DATA_KEY);

        return cache.getNextId();
    }

    private synchronized long getNextId() {
        setDirty();
        return ++this.lastId;
    }
}