package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrAnimationControllerLoader extends BrResourcesLoader {
    public static final BrAnimationControllerLoader INSTANCE = new BrAnimationControllerLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationControllerLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrAnimationControllers> animationControllers = new HashMap<>();

    private BrAnimationControllerLoader() {
        super("animation_controllers", "json");
    }

    @Nullable
    public static BrAnimationControllers getController(ResourceLocation location) {
        return INSTANCE.animationControllers.get(location);
    }

    public static Map<ResourceLocation, BrAnimationControllers> getAnimationControllers() {
        return INSTANCE.animationControllers;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animationControllers.clear();

        pObject.forEach((key, value) -> {
            try {
                animationControllers.put(key, BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, value).getOrThrow(false, LOGGER::warn));
            } catch (Exception e) {
                LOGGER.error("can't load animation controller {}", key, e);
            }
        });
        AnimationAssetRegistry.replaceAssets(BrAnimationLoader.getAnimations(), animationControllers);
    }
}
