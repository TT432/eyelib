package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.util.search.Searchable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@Slf4j
@Getter
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BrClientEntityLoader extends BrResourcesLoader implements Searchable<BrClientEntity> {
    public static final BrClientEntityLoader INSTANCE = new BrClientEntityLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrClientEntityLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrClientEntity> entities = new HashMap<>();

    public BrClientEntity get(ResourceLocation id) {
        return entities.get(id);
    }

    public void put(ResourceLocation id, BrClientEntity entity) {
        entities.put(id, entity);
        MinecraftForge.EVENT_BUS.post(new ManagerEntryChangedEvent(getManagerName(), id.toString(), entity));
    }

    public String getManagerName() {
        return "BrClientEntityLoader";
    }

    private BrClientEntityLoader() {
        super("entity", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        entities.clear();

        for (var entry : object.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow(false, LOGGER::warn);
                entities.put(new ResourceLocation(entity.identifier()), entity);
            } catch (Exception e) {
                log.error("can't load entity {}", key, e);
            }
        }
    }

    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return entities.entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey().toString(), searchStr))
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue()));
    }
}
