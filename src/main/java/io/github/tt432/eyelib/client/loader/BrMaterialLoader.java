package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.material.BrMaterial;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
@Getter
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BrMaterialLoader extends BrResourcesLoader {
    public static final BrMaterialLoader INSTANCE = new BrMaterialLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrMaterial> materials = new HashMap<>();

    private BrMaterialLoader() {
        super("materials", "material");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        materials.clear();

        for (var entry : object.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                materials.put(key, BrMaterial.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow());
            } catch (Exception e) {
                log.error("can't load material {}", key, e);
            }
        }

        for (BrMaterial value : materials.values()) {
            value.materials().forEach((s, m) -> Eyelib.getMaterialManager().put(s, m));
        }
    }
}
