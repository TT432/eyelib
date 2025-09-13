package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
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
public class BrAnimationLoader extends BrResourcesLoader {
    public static final BrAnimationLoader INSTANCE = new BrAnimationLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrAnimation> animations = new HashMap<>();

    private BrAnimationLoader() {
        super("animations", "json");
    }

    public static BrAnimation getAnimation(ResourceLocation resourceLocation) {
        return INSTANCE.animations.get(resourceLocation);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animations.clear();

        pObject.forEach((rl, json) -> {
            try {
                animations.put(rl, BrAnimation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (Exception e) {
                log.error("can't load animation {}", rl, e);
            }
        });

        for (BrAnimation value : animations.values()) {
            value.animations().forEach((s, a) -> Eyelib.getAnimationManager().put(s, a));
        }
    }
}
