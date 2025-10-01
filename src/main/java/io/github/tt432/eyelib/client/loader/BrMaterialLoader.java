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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
@Getter
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BrMaterialLoader extends BrResourcesLoader {
    public static final BrMaterialLoader INSTANCE = new BrMaterialLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrMaterialLoader.class);

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
                materials.put(key, BrMaterial.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow(false, LOGGER::warn));
            } catch (Exception e) {
                log.error("can't load material {}", key, e);
            }
        }

        for (BrMaterial value : materials.values()) {
            value.materials().forEach((s, m) -> Eyelib.getMaterialManager().put(s, m));
        }
    }
}
