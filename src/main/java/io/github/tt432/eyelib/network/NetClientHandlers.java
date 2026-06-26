package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import io.github.tt432.eyelib.bridge.particle.ParticleRuntimeBridge;
import io.github.tt432.eyelib.bridge.network.particle.RemoveParticlePacket;
import io.github.tt432.eyelib.bridge.network.particle.SpawnParticlePacket;
import io.github.tt432.eyelib.bridge.network.animation.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.bridge.network.model.ModelComponentSyncPacket;
/**
 * @author TT432
 */
public class NetClientHandlers {
    // <editor-fold desc="Client handlers">

    public static void onModelComponentSyncPacket(ModelComponentSyncPacket packet) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket packet) {
        ClientRenderSyncService.apply(packet);
    }

    public static void onRemoveParticlePacket(RemoveParticlePacket packet) {
        ParticleRuntimeBridge.SPAWN_ADAPTER.remove(packet.removeId());
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
        ParticleRuntimeBridge.SPAWN_ADAPTER.spawn(
                new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position())
        );
    }

    // </editor-fold>
}
