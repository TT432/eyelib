package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
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
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BrAnimationControllerLoader extends SimpleJsonResourceReloadListener {
    public static final BrAnimationControllerLoader INSTANCE = new BrAnimationControllerLoader();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrAnimationControllers> animationControllers = new HashMap<>();

    private BrAnimationControllerLoader() {
        super(new Gson(), "animation_controllers");
    }

    public static BrAnimationControllers getController(ResourceLocation location) {
        return INSTANCE.animationControllers.get(location);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animationControllers.clear();

        pObject.forEach((key, value) -> animationControllers.put(key, BrAnimationControllers.parse(key.toString(), value.getAsJsonObject())));
    }
}
