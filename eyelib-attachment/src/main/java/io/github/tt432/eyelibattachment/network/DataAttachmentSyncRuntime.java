package io.github.tt432.eyelibattachment.network;

import io.github.tt432.eyelibattachment.dataattach.DataAttachmentType;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentContainerCapability;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibattachment.dataattach.mc.McDataAttachmentContainer;
import io.github.tt432.eyelibnetwork.EyelibNetworkTransport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 数据附属网络同步的运行时入口。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataAttachmentSyncRuntime {
    public static <C> void syncTrackedAndSelf(DataAttachmentType<C> attachment, Entity entity, C value) {
        EyelibNetworkTransport.sendToTrackedAndSelf(entity, new DataAttachmentUpdatePacket<>(entity.getId(), attachment, value));
    }

    public static <C> void syncToPlayer(DataAttachmentType<C> attachment, Entity entity, C value, ServerPlayer player) {
        EyelibNetworkTransport.sendToPlayer(player, UniDataUpdatePacket.crate(entity.getId(), attachment, value));
    }

    public static void syncContainer(Entity entity, CompoundTag data) {
        EyelibNetworkTransport.sendToTrackedAndSelf(entity, new DataAttachmentSyncPacket(entity.getId(), data));
    }

    public static void applyUpdate(DataAttachmentUpdatePacket<?> packet) {
        applyAttachmentUpdate(DataAttachmentSyncPayloadOps.from(packet));
    }

    public static <T> void applyUpdate(UniDataUpdatePacket<T> packet) {
        applyAttachmentUpdate(DataAttachmentSyncPayloadOps.from(packet));
    }

    public static void applyExtraEntityData(ExtraEntityDataPacket packet) {
        applyAttachmentUpdate(DataAttachmentSyncPayloadOps.from(packet));
    }

    public static void applyExtraEntityUpdateData(ExtraEntityUpdateDataPacket packet) {
        applyAttachmentUpdate(DataAttachmentSyncPayloadOps.from(packet));
    }

    public static void applySync(DataAttachmentSyncPacket packet) {
        var entity = clientEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        var cap = entity.getCapability(DataAttachmentContainerCapability.INSTANCE);
        cap.ifPresent(container -> {
            if (container instanceof McDataAttachmentContainer mcContainer) {
                mcContainer.deserializeNBT(packet.data());
            }
        });
    }

    public static void handleDestroyInfoUpdate(UpdateDestroyInfoPacket packet, ServerPlayer player) {
        var key = DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get();
        var data = DataAttachmentHelper.getOrCreate(key, player);
        var updated = DataAttachmentSyncPayloadOps.withDigState(data, packet.dig());
        if (updated != data) {
            DataAttachmentHelper.setLocal(key, player, updated);
            EyelibNetworkTransport.sendToTrackedAndSelf(player, new ExtraEntityDataPacket(player.getId(), updated));
        }
    }

    private static <T> void applyAttachmentUpdate(DataAttachmentSyncPayloadOps.AttachmentUpdate<T> update) {
        var entity = clientEntity(update.entityId());
        if (entity == null) {
            return;
        }

        DataAttachmentHelper.setLocal(update.attachment(), entity, update.value());
    }

    private static @Nullable Entity clientEntity(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return level.getEntity(entityId);
    }
}