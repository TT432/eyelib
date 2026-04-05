package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistry;
import io.github.tt432.eyelib.util.search.Searchable;
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
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrClientEntityLoader extends BrResourcesLoader {
    public static final BrClientEntityLoader INSTANCE = new BrClientEntityLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrClientEntityLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private BrClientEntityLoader() {
        super("entity", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BrClientEntity> parsedEntities = new HashMap<>();

        for (var entry : object.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow(false, LOGGER::warn);
                parsedEntities.put(new ResourceLocation(entity.identifier()), entity);
            } catch (Exception e) {
                LOGGER.error("can't load entity {}", key, e);
            }
        }

        ClientEntityAssetRegistry.replaceClientEntities(parsedEntities);
    }
}
