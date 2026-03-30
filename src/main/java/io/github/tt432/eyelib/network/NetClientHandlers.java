package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelib.network.dataattach.DataAttachmentSyncService;
import net.minecraftforge.network.NetworkEvent;

/**
 * @author TT432
 */
public class NetClientHandlers {
    // <editor-fold desc="Client handlers">

    public static void onModelComponentSyncPacket(ModelComponentSyncPacket packet, NetworkEvent.Context context) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket packet, NetworkEvent.Context context) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onRemoveParticlePacket(RemoveParticlePacket packet, NetworkEvent.Context context) {
        ParticleSpawnService.removeEmitter(packet.removeId());
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket packet, NetworkEvent.Context context) {
        ParticleSpawnService.spawnFromPacket(packet);
    }

    public static void onExtraEntityUpdateDataPacket(ExtraEntityUpdateDataPacket packet, NetworkEvent.Context context) {
        DataAttachmentSyncService.applyExtraEntityUpdateData(packet);
    }

    public static <T> void onUniDataUpdatePacket(UniDataUpdatePacket<T> packet, NetworkEvent.Context context) {
        DataAttachmentSyncService.applyUpdate(packet);
    }

    public static void onExtraEntityDataPacket(ExtraEntityDataPacket packet, NetworkEvent.Context context) {
        DataAttachmentSyncService.applyExtraEntityData(packet);
    }

    public static <C> void onDataAttachmentUpdatePacket(DataAttachmentUpdatePacket<C> packet, NetworkEvent.Context context) {
        DataAttachmentSyncService.applyUpdate(packet);
    }

    public static void onDataAttachmentSyncPacket(DataAttachmentSyncPacket packet, NetworkEvent.Context context) {
        DataAttachmentSyncService.applySync(packet);
    }

    // </editor-fold>

}
