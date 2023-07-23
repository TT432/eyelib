package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
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
public class BrAnimationLoader extends SimpleJsonResourceReloadListener {
    public static final BrAnimationLoader INSTANCE = new BrAnimationLoader(new Gson(), "animations/bedrock");

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Getter
    Map<ResourceLocation, BrAnimation> animations = new HashMap<>();

    private BrAnimationLoader(Gson pGson, String pDirectory) {
        super(pGson, pDirectory);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        animations = pObject.entrySet().stream()
                .map(entry -> {
                    BrAnimation parse = BrAnimation.parse(entry.getKey().toString(), entry.getValue().getAsJsonObject());

                    if (parse == null)
                        return null;
                    else
                        return Map.entry(entry.getKey(), parse);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
