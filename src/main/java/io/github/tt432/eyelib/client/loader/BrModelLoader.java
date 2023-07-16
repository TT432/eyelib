package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrModelLoader extends SimpleJsonResourceReloadListener {
    public static final BrModelLoader INSTANCE = new BrModelLoader(new Gson(), "models/bedrock");

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener( INSTANCE);
    }

    @Getter
    Map<ResourceLocation, BrModel> models = new HashMap<>();

    private BrModelLoader(Gson pGson, String pDirectory) {
        super(pGson, pDirectory);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        models = pObject.entrySet().stream()
                .map(entry -> {
                    BrModel parse = BrModel.parse(entry.getKey().toString(), entry.getValue().getAsJsonObject());

                    if (parse == null)
                        return null;
                    else
                        return Map.entry(entry.getKey(), parse);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
