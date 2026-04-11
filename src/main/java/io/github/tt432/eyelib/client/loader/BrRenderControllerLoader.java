package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Getter
@Slf4j
public class BrRenderControllerLoader extends BrResourcesLoader {
    public static final BrRenderControllerLoader INSTANCE = new BrRenderControllerLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrRenderControllerLoader.class);

    private final Map<ResourceLocation, RenderControllers> controllers = new HashMap<>();

    private BrRenderControllerLoader() {
        super("render_controllers", "json");
    }

    @Nullable
    public static RenderControllers get(ResourceLocation resourceLocation) {
        return INSTANCE.controllers.get(resourceLocation);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        Map<ResourceLocation, RenderControllers> parsedRenderControllers =
                LoaderParsingOps.parseBySourceKey(pObject, RenderControllers.CODEC, LOGGER, "render controller");
        controllers.clear();
        controllers.putAll(parsedRenderControllers);
        RenderControllerAssetRegistry.replaceRenderControllers(controllers);
    }
}
