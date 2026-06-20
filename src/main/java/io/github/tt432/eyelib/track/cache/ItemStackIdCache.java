package io.github.tt432.eyelib.track.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.track.EyelibTrack;
//? if <1.20.6 {
//?} else {
import net.minecraft.core.HolderLookup;
//?}
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

    //? if <1.20.6 {
    private static ItemStackIdCache load(CompoundTag tag) {
    //?} else {
    private static ItemStackIdCache load(CompoundTag tag, HolderLookup.Provider provider) {
    //?}
        ItemStackIdCache cache = new ItemStackIdCache();
        //? if <26.1 {
        cache.lastId = tag.getLong("lastId");
        //?} else {
        cache.lastId = tag.getLong("lastId").orElse(0L);
        //?}
        return cache;
    }

    //? if <26.1 {
    @Override
    //?}
    //? if <1.20.6 {
    public CompoundTag save(CompoundTag tag) {
    //?} else {
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    //?}
        tag.putLong("lastId", lastId);
        return tag;
    }

    /**
     * 分配一个新的全局唯一追踪 ID。
     */
    public static long getFreeId(ServerLevel level) {
        //? if <26.1 {
        ItemStackIdCache cache = level.getServer().overworld()
                .getDataStorage()
                //? if <1.20.6 {
                .computeIfAbsent(ItemStackIdCache::load, ItemStackIdCache::new, DATA_KEY);
                //?} else {
                .computeIfAbsent(new SavedData.Factory<>(ItemStackIdCache::new, ItemStackIdCache::load), DATA_KEY);
                //?}
        //?} else {
        ItemStackIdCache cache = new ItemStackIdCache();
        //?}

        return cache.getNextId();
    }

    private synchronized long getNextId() {
        setDirty();
        return ++this.lastId;
    }
}
