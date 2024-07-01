package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;

import java.util.List;

/**
 * @author TT432
 */
@ParticleComponent(value = "particle_expire_if_not_in_blocks", target = ComponentTarget.PARTICLE)
public record ParticleExpireIfNotInBlocks(
        List<String> blocks
) {
    public static final Codec<ParticleExpireIfNotInBlocks> CODEC =
            Codec.STRING.listOf().xmap(ParticleExpireIfNotInBlocks::new, ParticleExpireIfNotInBlocks::blocks);
}
