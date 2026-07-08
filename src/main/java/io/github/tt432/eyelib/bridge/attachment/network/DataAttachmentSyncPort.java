package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.bridge.attachment.network.adapter.DataAttachmentSyncRuntime;
import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.bridge.attachment.network.ExtraEntityDataPacket;
import io.github.tt432.eyelib.bridge.attachment.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.bridge.attachment.network.UniDataUpdatePacket;
import io.github.tt432.eyelib.bridge.attachment.network.UpdateDestroyInfoPacket;
import net.minecraft.server.level.ServerPlayer;

public interface DataAttachmentSyncPort {
    static void handleDestroyInfoUpdate(UpdateDestroyInfoPacket packet, ServerPlayer player) {
        DataAttachmentSyncRuntime.handleDestroyInfoUpdate(packet, player);
    }

    static void applyExtraEntityUpdateData(ExtraEntityUpdateDataPacket packet) {
        DataAttachmentSyncRuntime.applyExtraEntityUpdateData(packet);
    }

    static <T> void applyUpdate(UniDataUpdatePacket<T> packet) {
        DataAttachmentSyncRuntime.applyUpdate(packet);
    }

    static void applyExtraEntityData(ExtraEntityDataPacket packet) {
        DataAttachmentSyncRuntime.applyExtraEntityData(packet);
    }

    static void applyUpdate(DataAttachmentUpdatePacket<?> packet) {
        DataAttachmentSyncRuntime.applyUpdate(packet);
    }

    static void applySync(DataAttachmentSyncPacket packet) {
        DataAttachmentSyncRuntime.applySync(packet);
    }
}
