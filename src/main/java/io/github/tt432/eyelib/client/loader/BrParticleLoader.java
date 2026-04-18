package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.registry.ParticleAssetRegistry;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public class BrParticleLoader extends BrResourcesLoader {
    public static final BrParticleLoader INSTANCE = new BrParticleLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrParticleLoader.class);

    private final Map<ResourceLocation, BrParticle> particles = new HashMap<>();

    @Nullable
    public static BrParticle getParticle(ResourceLocation location) {
        return INSTANCE.particles.get(location);
    }

    private BrParticleLoader() {
        super("particles", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, BrParticle> parsedParticles =
                LoaderParsingOps.parseBySourceKey(pObject, BrParticle.CODEC, LOGGER, "particle");
        particles.clear();
        particles.putAll(parsedParticles);
        ParticleAssetRegistry.replaceParticles(particles);
    }
}

