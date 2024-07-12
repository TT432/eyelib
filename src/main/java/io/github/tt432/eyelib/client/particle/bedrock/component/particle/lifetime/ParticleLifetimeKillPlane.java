package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector4f;

/**
 * todo
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_kill_plane", target = ComponentTarget.PARTICLE)
public record ParticleLifetimeKillPlane(
        Vector4f plane
) implements ParticleParticleComponent {
    public static final Codec<ParticleLifetimeKillPlane> CODEC =
            ExtraCodecs.VECTOR4F.xmap(ParticleLifetimeKillPlane::new, ParticleLifetimeKillPlane::plane);
}
