package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleLookup {
    public static @Nullable BrParticle get(ResourceLocation id) {
        return ParticleManager.readPort().get(id.toString());
    }

    public static @Nullable BrParticle get(String id) {
        return ParticleManager.readPort().get(id);
    }

    public static Collection<String> names() {
        return ParticleManager.readPort().getAllData().keySet();
    }
}
