package io.github.tt432.eyelib.track.api;

import io.github.tt432.eyelib.track.EyelibTrack;
import io.github.tt432.eyelib.track.cache.ItemStackIdCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * ItemStack 追踪 ID 的获取与分配。
 *
 * @author TT432
 */
public final class ItemTrackApi {

    private ItemTrackApi() {
    }

    /**
     * 从 ItemStack NBT 中读取追踪 ID。
     *
     * @return 已分配 ID，或 {@code stack.hashCode()} 兜底
     */
    public static long getId(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(EyelibTrack.TRACK_ID_KEY, Tag.TAG_LONG)) {
            return tag.getLong(EyelibTrack.TRACK_ID_KEY);
        }

        return (long) stack.hashCode();
    }

    /**
     * 读取或分配追踪 ID（仅服务端调用）。
     */
    public static long getOrAssignId(ItemStack stack, ServerLevel level) {
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains(EyelibTrack.TRACK_ID_KEY, Tag.TAG_LONG)) {
            tag.putLong(EyelibTrack.TRACK_ID_KEY, ItemStackIdCache.getFreeId(level));
        }

        return tag.getLong(EyelibTrack.TRACK_ID_KEY);
    }

    /**
     * 比较两个 ItemStack 是否 ID 一致（用于严格同步场景）。
     */
    public static boolean hasSameTrackId(ItemStack a, ItemStack b) {
        return getId(a) == getId(b);
    }

    /**
     * 从 ItemStack NBT 中移除追踪 ID。
     */
    public static void removeTrackId(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(EyelibTrack.TRACK_ID_KEY)) {
            tag.remove(EyelibTrack.TRACK_ID_KEY);

            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}