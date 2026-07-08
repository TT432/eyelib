package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentSyncPort;
import io.github.tt432.eyelib.bridge.network.animation.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.bridge.attachment.network.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.bridge.attachment.network.ExtraEntityDataPacket;
import io.github.tt432.eyelib.bridge.attachment.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.bridge.attachment.network.UniDataUpdatePacket;
import io.github.tt432.eyelib.bridge.attachment.network.UpdateDestroyInfoPacket;
import io.github.tt432.eyelib.bridge.network.model.ModelComponentSyncPacket;
import io.github.tt432.eyelib.bridge.network.adapter.EyelibNetworkTransport;
import io.github.tt432.eyelib.bridge.network.particle.RemoveParticlePacket;
import io.github.tt432.eyelib.bridge.network.particle.SpawnParticlePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibNetworkManager {
    public static void register() {
        EyelibNetworkTransport.registerServerPacket(
                UpdateDestroyInfoPacket.class,
                UpdateDestroyInfoPacket.STREAM_CODEC::encode,
                UpdateDestroyInfoPacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::handleDestroyInfoUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityUpdateDataPacket.class,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::encode,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::applyExtraEntityUpdateData
        );

        EyelibNetworkTransport.registerClientPacket(
                UniDataUpdatePacket.class,
                UniDataUpdatePacket.STREAM_CODEC::encode,
                UniDataUpdatePacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::applyUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityDataPacket.class,
                ExtraEntityDataPacket.STREAM_CODEC::encode,
                ExtraEntityDataPacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::applyExtraEntityData
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentUpdatePacket.class,
                DataAttachmentUpdatePacket.STREAM_CODEC::encode,
                DataAttachmentUpdatePacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::applyUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentSyncPacket.class,
                DataAttachmentSyncPacket.STREAM_CODEC::encode,
                DataAttachmentSyncPacket.STREAM_CODEC::decode,
                DataAttachmentSyncPort::applySync
        );

        EyelibNetworkTransport.registerClientPacket(
                ModelComponentSyncPacket.class,
                ModelComponentSyncPacket.STREAM_CODEC::encode,
                ModelComponentSyncPacket.STREAM_CODEC::decode,
                NetClientHandlers::onModelComponentSyncPacket
        );

        EyelibNetworkTransport.registerClientPacket(
                AnimationComponentSyncPacket.class,
                AnimationComponentSyncPacket.STREAM_CODEC::encode,
                AnimationComponentSyncPacket.STREAM_CODEC::decode,
                NetClientHandlers::onAnimationComponentSyncPacket
        );

        EyelibNetworkTransport.registerClientPacket(
                RemoveParticlePacket.class,
                RemoveParticlePacket.STREAM_CODEC::encode,
                RemoveParticlePacket.STREAM_CODEC::decode,
                NetClientHandlers::onRemoveParticlePacket
        );

        EyelibNetworkTransport.registerClientPacket(
                SpawnParticlePacket.class,
                SpawnParticlePacket.STREAM_CODEC::encode,
                SpawnParticlePacket.STREAM_CODEC::decode,
                NetClientHandlers::onSpawnParticlePacket
        );
    }

    public static void sendToServer(Object packet) {
        EyelibNetworkTransport.sendToServer(packet);
    }
}



