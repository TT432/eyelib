package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Getter
@Slf4j
@EventBusSubscriber(value = Dist.CLIENT)
public class BrAnimationControllerLoader extends BrResourcesLoader {
    public static final BrAnimationControllerLoader INSTANCE = new BrAnimationControllerLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrAnimationControllers> animationControllers = new HashMap<>();

    private BrAnimationControllerLoader() {
        super("animation_controllers", "json");
    }

    public static BrAnimationControllers getController(ResourceLocation location) {
        return INSTANCE.animationControllers.get(location);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animationControllers.clear();

        pObject.forEach((key, value) -> {
            try {
                animationControllers.put(key, BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, value).getOrThrow());
            } catch (Exception e) {
                log.error("can't load animation controller {}", key, e);
            }
        });

        for (var value : animationControllers.values()) {
            value.animationControllers().forEach((s, a) -> Eyelib.getAnimationManager().put(s, a));
        }
    }
}
