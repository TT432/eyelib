package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibparticle.loading.ParticleResourcePublication;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@Slf4j
@ResourceLoader

/** @author TT432 */
@NullMarked
public class BrParticleLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrParticleLoader.class);

    BrParticleLoader() {
        super("particles", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<String, JsonElement> resources = new LinkedHashMap<>();
        pObject.entrySet().forEach(entry -> resources.put(entry.getKey().toString(), entry.getValue()));
        ParticleResourcePublication.replaceFromJsonResources(resources, LOGGER);
    }
}
