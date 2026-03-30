package io.github.tt432.eyelib.network.dataattach;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.network.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import io.github.tt432.eyelib.network.ExtraEntityDataPacket;
import io.github.tt432.eyelib.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.network.UniDataUpdatePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataAttachmentSyncService {
    public static <C> void syncTrackedAndSelf(DataAttachmentType<C> attachment, Entity entity, C value) {
        EyelibNetworkManager.sendToTrackedAndSelf(entity, new DataAttachmentUpdatePacket<>(entity.getId(), attachment, value));
    }

    public static void syncContainer(Entity entity, CompoundTag data) {
        EyelibNetworkManager.sendToTrackedAndSelf(entity, new DataAttachmentSyncPacket(entity.getId(), data));
    }

    public static void applyUpdate(DataAttachmentUpdatePacket<?> packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        applyUnchecked(packet.attachment(), entity, packet.value());
    }

    public static void applySync(DataAttachmentSyncPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        var cap = entity.getCapability(DataAttachmentContainerCapability.INSTANCE);
        cap.ifPresent(container -> container.deserializeNBT(packet.data()));
    }

    public static <T> void applyUpdate(UniDataUpdatePacket<T> packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        applyToEntity(packet.type(), entity, packet.data());
    }

    public static void applyExtraEntityData(ExtraEntityDataPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        applyToEntity(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), entity, packet.data());
    }

    public static void applyExtraEntityUpdateData(ExtraEntityUpdateDataPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        applyToEntity(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), entity, packet.data());
    }

    @SuppressWarnings("unchecked")
    private static void applyUnchecked(DataAttachmentType<?> attachment, Entity entity, Object value) {
        applyToEntity((DataAttachmentType<Object>) attachment, entity, value);
    }

    private static <T> void applyToEntity(DataAttachmentType<T> attachment, Entity entity, T value) {
        if (entity == null) {
            return;
        }
        DataAttachmentHelper.setLocal(attachment, entity, value);
    }
}
