package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Slf4j
@ResourceLoader
public class BrRenderControllerLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrRenderControllerLoader.class);

    BrRenderControllerLoader() {
        super("render_controllers", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, RenderControllers> parsedRenderControllers =
                LoaderParsingOps.parseBySourceKey(pObject, RenderControllers.CODEC, LOGGER, "render controller");
        RenderControllerAssetRegistry.replaceRenderControllers(parsedRenderControllers);
    }
}
