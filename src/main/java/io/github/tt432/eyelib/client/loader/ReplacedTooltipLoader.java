package io.github.tt432.eyelib.client.loader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.gui.tooltip.ReplaceTooltipData;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.IOException;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ReplacedTooltipLoader implements ResourceManagerReloadListener {
    public static final ReplacedTooltipLoader INSTANCE = new ReplacedTooltipLoader();

    private static final Gson GSON = new Gson();

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Getter
    private ReplaceTooltipData data;

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        data = pResourceManager
                .getResource(ResourceLocations.of(Eyelib.MOD_ID, "replace_tooltip.json"))
                .map(r -> {
                    try {
                        return ReplaceTooltipData.CODEC
                                .parse(JsonOps.INSTANCE, GSON.fromJson(r.openAsReader(), JsonObject.class))
                                .getPartialOrThrow();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(null);
    }
}
