package io.github.tt432.eyelib.client.loader;

import Effekseer.swig.EffekseerBackendCore;
import Effekseer.swig.EffekseerManagerCore;
import io.github.tt432.eyelib.client.particle.effekseer.EfkefcObject;
import io.github.tt432.eyelib.util.SharedLibraryLoader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EfkefcLoader implements ResourceManagerReloadListener {
    public static final EfkefcLoader INSTANCE = new EfkefcLoader();

    public static final FileToIdConverter LISTER = new FileToIdConverter("efkefcs", ".efkefc");

    private EffekseerManagerCore core;
    private final Map<ResourceLocation, EfkefcObject> efkefcMap = new HashMap<>();

    public static EffekseerManagerCore getCore() {
        return INSTANCE.core;
    }

    public static Map<ResourceLocation, EfkefcObject> getEfkefcMap() {
        return INSTANCE.efkefcMap;
    }

    private void init() {
        new SharedLibraryLoader().load("EffekseerNativeForJava");
        EffekseerBackendCore.InitializeWithOpenGL();

        core = new EffekseerManagerCore();
        core.Initialize(80000);
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        efkefcMap.clear();
        LISTER.listMatchingResources(pResourceManager).forEach((key, resource) -> {
            key = LISTER.fileToId(key);

            try (var is = resource.open()) {
                efkefcMap.put(key, new EfkefcObject(key, pResourceManager, is));
            } catch (Exception exception) {
                log.error("Failed to load efkefc {}", key, exception);
            }
        });
    }

    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        INSTANCE.init();
        event.registerReloadListener(INSTANCE);
    }
}
