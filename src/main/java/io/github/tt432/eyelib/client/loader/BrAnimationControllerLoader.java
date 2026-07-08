package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
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
    //? if <26.1 {
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    //?} else {
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    //?}
        //? if <26.1 {
        Map<ResourceLocation, BrAnimationControllerSet> parsedSchemaSets =
        //?} else {
        Map<Identifier, BrAnimationControllerSet> parsedSchemaSets =
        //?}
                LoaderParsingOps.parseBySourceKey(pObject, BrAnimationControllerSet.CODEC, LOGGER, "animation controller");
        //? if <26.1 {
        LinkedHashMap<ResourceLocation, BrAnimationControllers> parsedControllers = new LinkedHashMap<>();
        //?} else {
        LinkedHashMap<Identifier, BrAnimationControllers> parsedControllers = new LinkedHashMap<>();
        //?}
        parsedSchemaSets.forEach((location, schemaSet) -> parsedControllers.put(location, BrAnimationControllers.fromSchemaSet(schemaSet)));
        AnimationAssetRegistry.stageControllers(parsedControllers);
    }
}
