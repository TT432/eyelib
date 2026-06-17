package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;

import java.util.List;

/** @author TT432 */
public record ParticleExpireIfInBlocks(
        List<String> blocks
) implements ParticleParticleComponent {
    public static final Codec<ParticleExpireIfInBlocks> CODEC =
            Codec.STRING.listOf().xmap(ParticleExpireIfInBlocks::new, ParticleExpireIfInBlocks::blocks);

    @Override
    public void onFrame(ParticleAccess particle) {
        particle.blockAtPosition().filter(blocks::contains).ifPresent(ignored -> particle.remove());
    }
}