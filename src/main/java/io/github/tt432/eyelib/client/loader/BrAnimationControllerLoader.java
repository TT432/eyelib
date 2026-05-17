package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibanimation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@ResourceLoader
public class BrAnimationControllerLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationControllerLoader.class);

    BrAnimationControllerLoader() {
        super("animation_controllers", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, BrAnimationControllerSet> parsedSchemaSets =
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationControllerSet.CODEC, LOGGER, "animation controller");
        LinkedHashMap<ResourceLocation, BrAnimationControllers> parsedControllers = new LinkedHashMap<>();
        parsedSchemaSets.forEach((location, schemaSet) -> parsedControllers.put(location, BrAnimationControllers.fromSchemaSet(schemaSet)));
        AnimationAssetRegistry.stageControllers(parsedControllers);
    }
}
