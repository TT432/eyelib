package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * 加载 animations 目录下的动画 JSON 文件。
 *
 * @author TT432
 */
@ResourceLoader
@NullMarked
public class BrAnimationLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationLoader.class);

    BrAnimationLoader() {
        super("animations", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, BrAnimationSet> parsedSchemaSets =
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationSet.CODEC, LOGGER, "animation");
        Map<ResourceLocation, BrAnimation> parsedAnimations = new HashMap<>();
        parsedSchemaSets.forEach((location, schemaSet) -> parsedAnimations.put(location, BrAnimation.fromSchemaSet(schemaSet)));
        AnimationAssetRegistry.stageAnimations(parsedAnimations);
    }
}
