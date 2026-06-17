package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.particle.loading.ParticleResourcePublication;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Forge 资源重载系统的粒子适配器。将 particles/*.json 文件通过
 * {@link ParticleResourcePublication#replaceFromJsonResources(Map, org.slf4j.Logger)}
 * 发布到粒子模块。此 Bridge 必须保留在根模块中，eyelib-particle 的
 * ParticleResourcePublication 不提供 Forge ReloadListener 注册。
 *
 * @author TT432
 */
@Slf4j
@ResourceLoader
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
