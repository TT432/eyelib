package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.material.BrMaterial;
import io.github.tt432.eyelib.client.registry.MaterialAssetRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
@Getter
public class BrMaterialLoader extends BrResourcesLoader {
    public static final BrMaterialLoader INSTANCE = new BrMaterialLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrMaterialLoader.class);

    private final Map<ResourceLocation, BrMaterial> materials = new HashMap<>();

    private BrMaterialLoader() {
        super("materials", "material");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BrMaterial> parsedMaterials =
                LoaderParsingOps.parseBySourceKey(object, BrMaterial.CODEC, LOGGER, "material");
        materials.clear();
        materials.putAll(parsedMaterials);
        MaterialAssetRegistry.replaceMaterials(materials);
    }
}
