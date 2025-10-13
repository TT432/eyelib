package io.github.tt432.eyelib.client.particle.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import net.minecraft.Util;
import org.joml.Vector4f;

import java.util.List;

/**
 * todo
 *
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_kill_plane", target = ComponentTarget.PARTICLE)
public record ParticleLifetimeKillPlane(
        Vector4f plane
) implements ParticleParticleComponent {
    public static final Codec<Vector4f> VECTOR4F = Codec.FLOAT
            .listOf()
            .comapFlatMap(
                    p_337575_ -> Util.fixedSize((List<Float>) p_337575_, 4)
                            .map(p_340675_ -> new Vector4f(p_340675_.get(0), p_340675_.get(1), p_340675_.get(2), p_340675_.get(3))),
                    p_340674_ -> List.of(p_340674_.x(), p_340674_.y(), p_340674_.z(), p_340674_.w())
            );
    public static final Codec<ParticleLifetimeKillPlane> CODEC =
            VECTOR4F.xmap(ParticleLifetimeKillPlane::new, ParticleLifetimeKillPlane::plane);
}
