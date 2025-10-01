package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Getter
@Slf4j
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BrRenderControllerLoader extends BrResourcesLoader {
    public static final BrRenderControllerLoader INSTANCE = new BrRenderControllerLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrRenderControllerLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, RenderControllers> controllers = new HashMap<>();

    private BrRenderControllerLoader() {
        super("render_controllers", "json");
    }

    public static RenderControllers get(ResourceLocation resourceLocation) {
        return INSTANCE.controllers.get(resourceLocation);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        controllers.clear();

        pObject.forEach((id, obj) -> {
            try {
                if (id.getPath().endsWith("render_controllers")) {
                    controllers.put(id, RenderControllers.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, LOGGER::warn));
                }
            } catch (Exception e) {
                log.error("Failed to parse render controller {}: {}", id, obj.toString(), e);
            }
        });

        for (RenderControllers value : controllers.values()) {
            value.render_controllers().forEach((n, r) -> Eyelib.getRenderControllerManager().put(n, r));
        }
    }
}
