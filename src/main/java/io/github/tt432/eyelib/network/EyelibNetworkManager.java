package io.github.tt432.eyelib.network;

import io.github.tt432.eyelibanimation.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelibattachment.network.*;
import io.github.tt432.eyelibmodel.network.packet.ModelComponentSyncPacket;
import io.github.tt432.eyelibnetwork.EyelibNetworkTransport;
import io.github.tt432.eyelibparticle.network.RemoveParticlePacket;
import io.github.tt432.eyelibparticle.network.SpawnParticlePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public class EyelibNetworkManager {
    public static void register() {
        EyelibNetworkTransport.registerServerPacket(
                UpdateDestroyInfoPacket.class,
                UpdateDestroyInfoPacket.STREAM_CODEC::encode,
                UpdateDestroyInfoPacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::handleDestroyInfoUpdate
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

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityUpdateDataPacket.class,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::encode,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::decode,
                NetClientHandlers::onExtraEntityUpdateDataPacket
        );

        EyelibNetworkTransport.registerClientPacket(
                UniDataUpdatePacket.class,
                UniDataUpdatePacket.STREAM_CODEC::encode,
                UniDataUpdatePacket.STREAM_CODEC::decode,
                NetClientHandlers::onUniDataUpdatePacket
        );

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityDataPacket.class,
                ExtraEntityDataPacket.STREAM_CODEC::encode,
                ExtraEntityDataPacket.STREAM_CODEC::decode,
                NetClientHandlers::onExtraEntityDataPacket
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentUpdatePacket.class,
                DataAttachmentUpdatePacket.STREAM_CODEC::encode,
                DataAttachmentUpdatePacket.STREAM_CODEC::decode,
                NetClientHandlers::onDataAttachmentUpdatePacket
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentSyncPacket.class,
                DataAttachmentSyncPacket.STREAM_CODEC::encode,
                DataAttachmentSyncPacket.STREAM_CODEC::decode,
                NetClientHandlers::onDataAttachmentSyncPacket
        );
    }

    public static void sendToServer(Object packet) {
        EyelibNetworkTransport.sendToServer(packet);
    }
}
