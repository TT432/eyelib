package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelibparticle.network.RemoveParticlePacket;
import io.github.tt432.eyelibparticle.network.SpawnParticlePacket;
import io.github.tt432.eyelibanimation.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelibmodel.network.packet.ModelComponentSyncPacket;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
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
