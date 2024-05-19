package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.render.define.RenderDefine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModelReplacerLoader extends SimpleJsonResourceReloadListener {
    private static final ModelReplacerLoader INSTANCE = new ModelReplacerLoader(new Gson(), "render_defines");

    public ModelReplacerLoader(Gson pGson, String pDirectory) {
        super(pGson, pDirectory);
    }

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, RenderDefine> renderDefines = new HashMap<>();
    private final Map<ResourceLocation, RenderDefine> byTarget = new HashMap<>();

    public static RenderDefine byId(ResourceLocation location) {
        return INSTANCE.renderDefines.get(location);
    }

    /**
     * @param location target id
     * @return last
     */
    public static RenderDefine byTarget(ResourceLocation location) {
        return INSTANCE.byTarget.get(location);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        renderDefines.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            RenderDefine parse = RenderDefine.parse(entry.getValue().getAsJsonObject());
            renderDefines.put(entry.getKey(), parse);
            byTarget.put(parse.target(), parse);
        }
    }
}
