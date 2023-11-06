package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrAnimationControllerLoader extends SimpleJsonResourceReloadListener {
    public static final BrAnimationControllerLoader INSTANCE =
            new BrAnimationControllerLoader(new Gson(), "animation_controllers");

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Getter
    private final Map<ResourceLocation, BrAnimationController> animationControllers = new HashMap<>();

    private BrAnimationControllerLoader(Gson pGson, String pDirectory) {
        super(pGson, pDirectory);
    }

    public static BrAnimationController getController(ResourceLocation location) {
        return INSTANCE.animationControllers.get(location);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        animationControllers.clear();

        pObject.forEach((key, value) ->
                animationControllers.put(key, BrAnimationController.parse(key.toString(), value.getAsJsonObject())));
    }
}
