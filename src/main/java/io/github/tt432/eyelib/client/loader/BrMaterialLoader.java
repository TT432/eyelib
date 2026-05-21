package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelib.client.registry.MaterialAssetRegistry;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

@Slf4j
@ResourceLoader

/** @author TT432 */
@NullMarked
public class BrMaterialLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrMaterialLoader.class);

    BrMaterialLoader() {
        super("materials", "material");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BrMaterial> parsedMaterials =
                LoaderParsingOps.parseBySourceKey(object, BrMaterial.CODEC, LOGGER, "material");
        MaterialAssetRegistry.replaceMaterials(parsedMaterials);
    }
}
