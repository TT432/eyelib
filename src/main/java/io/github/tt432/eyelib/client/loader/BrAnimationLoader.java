package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
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
public class BrAnimationLoader extends BrResourcesLoader {
    public static final BrAnimationLoader INSTANCE = new BrAnimationLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrAnimationLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrAnimation> animations = new HashMap<>();

    private BrAnimationLoader() {
        super("animations", "json");
    }

    @Nullable
    public static BrAnimation getAnimation(ResourceLocation resourceLocation) {
        return INSTANCE.animations.get(resourceLocation);
    }

    public static Map<ResourceLocation, BrAnimation> getAnimations() {
        return INSTANCE.animations;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animations.clear();

        pObject.forEach((rl, json) -> {
            try {
                animations.put(rl, BrAnimation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, LOGGER::warn));
            } catch (Exception e) {
                LOGGER.error("can't load animation {}", rl, e);
            }
        });
        AnimationAssetRegistry.replaceAssets(animations, BrAnimationControllerLoader.getAnimationControllers());
    }
}
