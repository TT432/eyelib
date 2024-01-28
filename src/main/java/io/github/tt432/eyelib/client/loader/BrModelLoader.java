package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.material.ModelMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrModelLoader extends SimpleJsonResourceReloadListener {
    private static final BrModelLoader INSTANCE = new BrModelLoader(new Gson(), "bedrock_models");

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrModel> models = new HashMap<>();
    private final Map<ResourceLocation, ModelMaterial> materials = new HashMap<>();

    public static BrModel getModel(ResourceLocation location) {
        return INSTANCE.models.get(location);
    }

    public static ModelMaterial getMaterial(ResourceLocation location) {
        return INSTANCE.materials.get(location);
    }

    private BrModelLoader(Gson pGson, String pDirectory) {
        super(pGson, pDirectory);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        models.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation key = entry.getKey();

            if (key.getPath().endsWith("material")) {
                materials.put(key, ModelMaterial.parse(entry.getValue().getAsJsonObject()));
            } else {
                models.put(key, BrModel.parse(key.toString(), entry.getValue().getAsJsonObject()));
            }
        }
    }
}
