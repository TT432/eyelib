package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 加载 animation_controllers 目录下的动画控制器 JSON 文件。
 *
 * @author TT432
 */
@ResourceLoader
public class BrAnimationControllerLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationControllerLoader.class);

    BrAnimationControllerLoader() {
        super("animation_controllers", "json");
    }

    @Override
    protected void applyJson(Map<String, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<String, BrAnimationControllerSet> parsedSchemaSets =
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationControllerSet.CODEC, LOGGER, "animation controller");
        LinkedHashMap<String, BrAnimationControllers> parsedControllers = new LinkedHashMap<>();
        parsedSchemaSets.forEach((location, schemaSet) -> parsedControllers.put(location, BrAnimationControllers.fromSchemaSet(schemaSet)));
        AnimationAssetRegistry.stageControllers(parsedControllers);
    }
}
