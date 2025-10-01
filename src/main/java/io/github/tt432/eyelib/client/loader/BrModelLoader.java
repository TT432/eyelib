package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.BrModelEntry;
import io.github.tt432.eyelib.util.search.Searchable;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@Slf4j
public class BrModelLoader extends BrResourcesLoader implements Searchable<BrModel> {
    public static final BrModelLoader INSTANCE = new BrModelLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrModel> models = new HashMap<>();

    @Override
    public Stream<Map.Entry<String, BrModel>> search(String searchStr) {
        return models.entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey().toString(), searchStr))
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue()));
    }

    public static BrModel getModel(ResourceLocation location) {
        return INSTANCE.models.get(location);
    }

    private BrModelLoader() {
        super("models", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        models.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation key = entry.getKey();

            try {
                models.put(key, BrModel.parse(entry.getValue().getAsJsonObject()));
            } catch (Exception e) {
                log.error("can't load model {}", key, e);
            }
        }

        for (BrModel value : models.values()) {
            for (BrModelEntry model : value.models()) {
                Eyelib.getModelManager().put(model.name().split(":")[0], model);
            }
        }
    }
}
