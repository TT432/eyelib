package io.github.tt432.eyelib.client.loader;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientLoaderLifecycleHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLoaderLifecycleHooks.class);

    private ClientLoaderLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        Type annotationType = Type.getType(ResourceLoader.class);

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations()) {
                if (!Objects.equals(annotationData.annotationType(), annotationType)) {
                    continue;
                }

                String memberName = annotationData.memberName();
                try {
                    Class<?> clazz = Class.forName(memberName);

                    if (!PreparableReloadListener.class.isAssignableFrom(clazz)) {
                        LOGGER.error("[ResourceLoader] {} is annotated with @ResourceLoader but does not implement PreparableReloadListener, skipping", memberName);
                        continue;
                    }

                    PreparableReloadListener instance = (PreparableReloadListener) clazz.getDeclaredConstructor()
                                                                                        .newInstance();
                    event.registerReloadListener(instance);
                } catch (Exception | LinkageError e) {
                    LOGGER.error("[ResourceLoader] Failed to load: {}", memberName, e);
                }
            }
        }

        event.registerReloadListener(new BedrockAddonAutoLoader());
    }
}
