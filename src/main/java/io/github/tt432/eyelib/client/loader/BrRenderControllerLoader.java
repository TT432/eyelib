package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.extern.slf4j.Slf4j;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * 加载 render_controllers 目录下的渲染控制器 JSON 文件。
 *
 * @author TT432
 */
@Slf4j
@ResourceLoader
public class BrRenderControllerLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrRenderControllerLoader.class);

    BrRenderControllerLoader() {
        super("render_controllers", "json");
    }

    @Override
    //? if <26.1 {
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    //?} else {
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    //?}
        //? if <26.1 {
        Map<ResourceLocation, RenderControllers> parsedRenderControllers =
        //?} else {
        Map<Identifier, RenderControllers> parsedRenderControllers =
        //?}
                LoaderParsingOps.parseBySourceKey(pObject, RenderControllers.CODEC, LOGGER, "render controller");
        for (RenderControllers value : parsedRenderControllers.values()) {
            value.render_controllers().forEach((key, entry) -> {
                RenderControllerEntry existing = RenderControllerManager.INSTANCE.get(key);
                if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                    return;
                }
                RenderControllerManager.INSTANCE.put(key, entry);
            });
        }
    }
}
