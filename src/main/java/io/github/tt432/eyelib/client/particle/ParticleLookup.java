package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleLookup {
    public static @Nullable BrParticle get(ResourceLocation id) {
        return Eyelib.getParticleManager().get(id.toString());
    }

    public static @Nullable BrParticle get(String id) {
        return Eyelib.getParticleManager().get(id);
    }
}
