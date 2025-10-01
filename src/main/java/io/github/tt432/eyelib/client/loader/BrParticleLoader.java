package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@Slf4j
public class BrParticleLoader extends BrResourcesLoader {
    private static final BrParticleLoader INSTANCE = new BrParticleLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrParticleLoader.class);

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    private final Map<ResourceLocation, BrParticle> particles = new HashMap<>();

    public static BrParticle getParticle(ResourceLocation location) {
        return INSTANCE.particles.get(location);
    }

    private BrParticleLoader() {
        super("particles", "json");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        particles.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation key = entry.getKey();
            try {
                particles.put(key, BrParticle.CODEC.parse(JsonOps.INSTANCE, entry.getValue().getAsJsonObject()).getOrThrow(false, LOGGER::warn));
            } catch (Exception e) {
                log.error("Failed to load particle {}", key, e);
            }
        }

        particles.forEach((k, v) -> Eyelib.getParticleManager().put(v.particleEffect().description().identifier(), v));
    }
}
