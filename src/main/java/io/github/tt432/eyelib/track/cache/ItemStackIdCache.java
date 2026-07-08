package io.github.tt432.eyelib.track.cache;

import io.github.tt432.eyelib.bridge.track.TrackIdStoragePort;
import io.github.tt432.eyelib.track.EyelibTrack;
import net.minecraft.server.level.ServerLevel;

/**
 * 服务端追踪 ID 计数器的应用层入口，委托 {@link TrackIdStoragePort} 处理跨版本
 * {@code SavedData} 差异，使本包不再携带版本条件化注释。
 *
 * @author TT432
 */
public final class ItemStackIdCache {
    private ItemStackIdCache() {
    }

    /**
     * 分配一个新的全局唯一追踪 ID。
     */
    public static long getFreeId(ServerLevel level) {
        return TrackIdStoragePort.getFreeId(level, EyelibTrack.TRACK_ID_KEY);
    }
}
