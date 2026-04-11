package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.importer.BedrockGeometryImporter;
import io.github.tt432.eyelib.client.registry.ModelAssetRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class BrModelLoader extends BrResourcesLoader {
    public static final BrModelLoader INSTANCE = new BrModelLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrModelLoader.class);

    private BrModelLoader() {
        super("models", "geo.json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        LinkedHashMap<String, Model> loadedModels = parseLoadedModels(pObject);
        ModelAssetRegistry.replaceModels(loadedModels);
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
