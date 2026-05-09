package io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.lifetime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import org.joml.Vector4f;

import java.util.List;

public record ParticleLifetimeKillPlane(
        Vector4f plane
) implements ParticleParticleComponent {
    public static final Codec<Vector4f> VECTOR4F = Codec.FLOAT.listOf().comapFlatMap(
            values -> values.size() == 4
                    ? DataResult.success(new Vector4f(values.get(0), values.get(1), values.get(2), values.get(3)))
                    : DataResult.error(() -> "Expected 4 floats for Vector4f"),
            value -> List.of(value.x(), value.y(), value.z(), value.w())
    );
    public static final Codec<ParticleLifetimeKillPlane> CODEC =
            VECTOR4F.xmap(ParticleLifetimeKillPlane::new, ParticleLifetimeKillPlane::plane);

    @Override
    public void onFrame(ParticleAccess particle) {
        float distance = plane.x() * particle.position().x()
                + plane.y() * particle.position().y()
                + plane.z() * particle.position().z()
                + plane.w();
        if (distance < 0) {
            particle.remove();
        }
    }
}
