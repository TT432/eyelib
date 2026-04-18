package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
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
public class BrAnimationLoader extends BrResourcesLoader {
    public static final BrAnimationLoader INSTANCE = new BrAnimationLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationLoader.class);

    private final Map<ResourceLocation, BrAnimation> animations = new HashMap<>();

    private BrAnimationLoader() {
        super("animations", "json");
    }

    @Nullable
    public static BrAnimation getAnimation(ResourceLocation resourceLocation) {
        return INSTANCE.animations.get(resourceLocation);
    }

    public static Map<ResourceLocation, BrAnimation> getAnimations() {
        return INSTANCE.animations;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, BrAnimationSet> parsedSchemaSets =
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationSet.CODEC, LOGGER, "animation");
        Map<ResourceLocation, BrAnimation> parsedAnimations = new HashMap<>();
        parsedSchemaSets.forEach((location, schemaSet) -> parsedAnimations.put(location, BrAnimation.fromSchemaSet(schemaSet)));
        animations.clear();
        animations.putAll(parsedAnimations);
        AnimationAssetRegistry.replaceAssets(animations, BrAnimationControllerLoader.getAnimationControllers());
    }
}

