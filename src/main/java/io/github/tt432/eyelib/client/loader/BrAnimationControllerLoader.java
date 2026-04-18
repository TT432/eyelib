package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class BrAnimationControllerLoader extends BrResourcesLoader {
    public static final BrAnimationControllerLoader INSTANCE = new BrAnimationControllerLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationControllerLoader.class);

    private final Map<ResourceLocation, BrAnimationControllers> animationControllers = new LinkedHashMap<>();

    private BrAnimationControllerLoader() {
        super("animation_controllers", "json");
    }

    @Nullable
    public static BrAnimationControllers getController(ResourceLocation location) {
        return INSTANCE.animationControllers.get(location);
    }

    public static Map<ResourceLocation, BrAnimationControllers> getAnimationControllers() {
        return INSTANCE.animationControllers;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, BrAnimationControllerSet> parsedSchemaSets =
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationControllerSet.CODEC, LOGGER, "animation controller");
        LinkedHashMap<ResourceLocation, BrAnimationControllers> parsedControllers = new LinkedHashMap<>();
        parsedSchemaSets.forEach((location, schemaSet) -> parsedControllers.put(location, BrAnimationControllers.fromSchemaSet(schemaSet)));
        animationControllers.clear();
        animationControllers.putAll(parsedControllers);
        AnimationAssetRegistry.replaceAssets(BrAnimationLoader.getAnimations(), animationControllers);
    }
}

