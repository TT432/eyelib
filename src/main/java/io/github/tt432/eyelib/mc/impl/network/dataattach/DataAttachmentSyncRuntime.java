package io.github.tt432.eyelib.mc.impl.network.dataattach;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.mc.impl.data_attach.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.mc.impl.data_attach.DataAttachmentHelper;
import io.github.tt432.eyelib.mc.impl.data_attach.McDataAttachmentContainer;
import io.github.tt432.eyelib.mc.impl.network.EyelibNetworkTransport;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UniDataUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UpdateDestroyInfoPacket;
import io.github.tt432.eyelib.network.dataattach.DataAttachmentSyncPayloadOps;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataAttachmentSyncRuntime {
    public static <C> void syncTrackedAndSelf(DataAttachmentType<C> attachment, Entity entity, C value) {
        EyelibNetworkTransport.sendToTrackedAndSelf(entity, new DataAttachmentUpdatePacket<>(entity.getId(), attachment, value));
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
        var key = EyelibAttachableData.EXTRA_ENTITY_DATA.get();
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

