package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelib.particle.api.ParticleSpawnRequest;
import io.github.tt432.eyelib.particle.client.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelib.particle.network.RemoveParticlePacket;
import io.github.tt432.eyelib.particle.network.SpawnParticlePacket;
import io.github.tt432.eyelib.animation.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.model.network.packet.ModelComponentSyncPacket;
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
        ParticleSpawnRuntimeAdapter.INSTANCE.remove(packet.removeId());
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
        ParticleSpawnRuntimeAdapter.INSTANCE.spawn(
                new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position())
        );
    }

    // </editor-fold>
}
