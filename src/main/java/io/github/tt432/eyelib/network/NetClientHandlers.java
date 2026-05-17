package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelibanimation.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.network.packet.ModelComponentSyncPacket;
import io.github.tt432.eyelibattachment.network.DataAttachmentSyncPacket;
import io.github.tt432.eyelibattachment.network.DataAttachmentSyncRuntime;
import io.github.tt432.eyelibattachment.network.DataAttachmentUpdatePacket;
import io.github.tt432.eyelibattachment.network.ExtraEntityDataPacket;
import io.github.tt432.eyelibattachment.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelibattachment.network.UniDataUpdatePacket;
import io.github.tt432.eyelibparticle.network.RemoveParticlePacket;
import io.github.tt432.eyelibparticle.network.SpawnParticlePacket;

public class NetClientHandlers {
    // <editor-fold desc="Client handlers">

    public static void onModelComponentSyncPacket(ModelComponentSyncPacket packet) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket packet) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onRemoveParticlePacket(RemoveParticlePacket packet) {
        ParticleSpawnService.removeEmitter(packet.removeId());
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
        ParticleSpawnService.spawnFromPacket(packet);
    }

    public static void onExtraEntityUpdateDataPacket(ExtraEntityUpdateDataPacket packet) {
        DataAttachmentSyncRuntime.applyExtraEntityUpdateData(packet);
    }

    public static <T> void onUniDataUpdatePacket(UniDataUpdatePacket<T> packet) {
        DataAttachmentSyncRuntime.applyUpdate(packet);
    }

    public static void onExtraEntityDataPacket(ExtraEntityDataPacket packet) {
        DataAttachmentSyncRuntime.applyExtraEntityData(packet);
    }

    public static <C> void onDataAttachmentUpdatePacket(DataAttachmentUpdatePacket<C> packet) {
        DataAttachmentSyncRuntime.applyUpdate(packet);
    }

    public static void onDataAttachmentSyncPacket(DataAttachmentSyncPacket packet) {
        DataAttachmentSyncRuntime.applySync(packet);
    }

    // </editor-fold>
}
