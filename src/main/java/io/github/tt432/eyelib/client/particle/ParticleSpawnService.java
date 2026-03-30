package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;
import io.github.tt432.eyelib.network.SpawnParticlePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleSpawnService {
    public static void spawnFromPacket(SpawnParticlePacket packet) {
        BrParticle particle = ParticleLookup.get(packet.particleId());
        if (particle == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return;
        }

        var data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
        BrParticleRenderManager.spawnEmitter(
                packet.spawnId(),
                new BrParticleEmitter(
                        particle,
                        data.getScope(),
                        Minecraft.getInstance().level,
                        packet.position()
                )
        );
    }

    public static void spawnEmitter(String spawnId, BrParticleEmitter emitter) {
        BrParticleRenderManager.spawnEmitter(spawnId, emitter);
    }

    public static void removeEmitter(String removeId) {
        BrParticleRenderManager.removeEmitter(removeId);
    }
}
