package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibimporter.model.importer.BedrockGeometryImporter;
import io.github.tt432.eyelibmodel.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 加载 models 目录下的几何模型 JSON 文件。
 *
 * @author TT432
 */
@ResourceLoader
@NullMarked
public class BrModelLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrModelLoader.class);

    BrModelLoader() {
        super("models", "geo.json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        LinkedHashMap<String, Model> loadedModels = parseLoadedModels(pObject);
        ModelManager.INSTANCE.replaceAll(new LinkedHashMap<>(loadedModels));
    }

    static LinkedHashMap<String, Model> parseLoadedModels(Map<ResourceLocation, JsonElement> sourceModels) {
        LinkedHashMap<String, Model> loadedModels = new LinkedHashMap<>();

        sourceModels.forEach((key, json) -> {
            try {
                if (!json.isJsonObject()) {
                    throw new IllegalArgumentException("Model file is not a JSON object");
                }

                loadedModels.putAll(BedrockGeometryImporter.importJson(json.getAsJsonObject()));
            } catch (Exception e) {
                LOGGER.error("can't load bedrock model {}", key, e);
            }
        });
        return loadedModels;
    }
}
