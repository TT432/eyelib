package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;
import io.github.tt432.eyelib.mc.impl.data_attach.DataAttachmentHelper;
import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;

/**
 * Transitional root runtime adapter for {@link ParticleSpawnApi}.
 * <p>
 * Packet adaptation delegates through the particle-module request API while Minecraft/capability/render-manager work stays
 * in root. Remove this facade after packet/runtime callers bind directly to particle API adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleSpawnService {
    private static final ParticleSpawnApi API = new RootParticleSpawnApi();

    public static ParticleSpawnApi api() {
        return API;
    }

    public static void spawnFromPacket(SpawnParticlePacket packet) {
        api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()));
    }

    public static void spawnEmitter(String spawnId, BrParticleEmitter emitter) {
        BrParticleRenderManager.spawnEmitter(spawnId, emitter);
    }

    public static void removeEmitter(String removeId) {
        api().remove(removeId);
    }

    private static final class RootParticleSpawnApi implements ParticleSpawnApi {
        @Override
        public void spawn(ParticleSpawnRequest request) {
            BrParticle particle = ParticleLookup.get(request.particleId());
            if (particle == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
                return;
            }

            RenderData<?> data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
            BrParticleRenderManager.spawnEmitter(
                    request.spawnId(),
                    new BrParticleEmitter(
                            particle,
                            data.getScope(),
                            Minecraft.getInstance().level,
                            request.position()
                    )
            );

        }

        @Override
        public void remove(String spawnId) {
            BrParticleRenderManager.removeEmitter(spawnId);
        }
    }
}
