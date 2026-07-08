package io.github.tt432.eyelib.bridge.track;

//? if <1.20.6 {
//?} else {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 追踪 ID 持久化 Port，屏蔽跨版本 {@link SavedData} API 差异。
 *
 * <p>本 Port 位于 bridge（ACL），承担版本特定 MC 接触；application 层通过
 * {@code io.github.tt432.eyelib.track.cache.ItemStackIdCache} 调用，避免在 application
 * 包内散落 {@code //?} 条件化注释。注册键以参数形式注入，避免 bridge 反向依赖 application。
 *
 * @author TT432
 */
public interface TrackIdStoragePort {

    /**
     * 分配一个新的全局唯一追踪 ID，持久化在存档中（26.1 暂为内存实现，保留原有行为）。
     *
     * @param dataKey SavedData 注册键
     */
    static long getFreeId(ServerLevel level, String dataKey) {
        //? if <26.1 {
        TrackIdData data = level.getServer().overworld()
                .getDataStorage()
                //? if <1.20.6 {
                .computeIfAbsent(TrackIdData::load, TrackIdData::new, dataKey);
                //?} else {
                .computeIfAbsent(new SavedData.Factory<>(TrackIdData::new, TrackIdData::load), dataKey);
                //?}
        //?} else {
        TrackIdData data = new TrackIdData();
        //?}
        return data.nextId();
    }

    /**
     * 单调递增的追踪 ID 计数器，由 {@link SavedData} 持久化。
     */
    final class TrackIdData extends SavedData {
        private long lastId;

        public TrackIdData() {
            this.lastId = 0;
        }

        //? if <1.20.6 {
        public static TrackIdData load(CompoundTag tag) {
        //?} else {
        public static TrackIdData load(CompoundTag tag, HolderLookup.Provider provider) {
        //?}
            TrackIdData data = new TrackIdData();
            //? if <26.1 {
            data.lastId = tag.getLong("lastId");
            //?} else {
            data.lastId = tag.getLong("lastId").orElse(0L);
            //?}
            return data;
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

        public synchronized long nextId() {
            setDirty();
            return ++this.lastId;
        }
    }
}
