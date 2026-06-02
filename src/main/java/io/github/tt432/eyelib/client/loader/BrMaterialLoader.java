package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
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
 * 加载 materials 目录下的材质文件。
 *
 * @author TT432
 */
@Slf4j
@ResourceLoader
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
        LinkedHashMap<String, BrMaterialEntry> flattened = new LinkedHashMap<>();
        for (BrMaterial value : parsedMaterials.values()) {
            value.materials().forEach(flattened::put);
        }
        MaterialManager.INSTANCE.replaceAll(flattened);
    }
}
