package io.github.tt432.eyelib.network;

import io.github.tt432.eyelibanimation.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelibmodel.network.packet.ModelComponentSyncPacket;
import io.github.tt432.eyelibnetwork.EyelibNetworkTransport;
import io.github.tt432.eyelibparticle.network.RemoveParticlePacket;
import io.github.tt432.eyelibparticle.network.SpawnParticlePacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class EyelibNetworkManager {
    public static void register() {
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
