package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
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
public class BrClientEntityLoader extends BrResourcesLoader {
    public static final BrClientEntityLoader INSTANCE = new BrClientEntityLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrClientEntity> entities = new HashMap<>();

    private BrClientEntityLoader() {
        super("entity", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        entities.clear();

        for (var entry : object.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow();
                entities.put(ResourceLocation.parse(entity.identifier()), entity);
            } catch (Exception e) {
                log.error("can't load entity {}", key, e);
            }
        }
    }
}
