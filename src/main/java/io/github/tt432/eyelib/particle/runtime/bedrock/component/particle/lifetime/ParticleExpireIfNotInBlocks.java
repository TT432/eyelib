package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;

import java.util.List;

/** @author TT432 */
public record ParticleExpireIfNotInBlocks(
        List<String> blocks
) implements ParticleParticleComponent {
    public static final Codec<ParticleExpireIfNotInBlocks> CODEC =
            Codec.STRING.listOf().xmap(ParticleExpireIfNotInBlocks::new, ParticleExpireIfNotInBlocks::blocks);

    @Override
    public void onFrame(ParticleAccess particle) {
        particle.blockAtPosition()
                .filter(block -> !blocks.contains(block))
                .ifPresent(ignored -> particle.remove());
    }
}