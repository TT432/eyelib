package io.github.tt432.eyelib.bridge.client.loader;

import io.github.tt432.eyelib.client.loader.ResourceLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.ModFileScanData;
//?} elif <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
//?}
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class ClientLoaderLifecycleHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLoaderLifecycleHooks.class);

    private ClientLoaderLifecycleHooks() {
    }

    //? if <26.1 {
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        registerAnnotatedListeners((key, listener) -> event.registerReloadListener(listener));
        event.registerReloadListener(new BedrockAddonAutoLoader());
    }
    //?} else {
    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        registerAnnotatedListeners((key, listener) ->
                event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", key), listener));
        event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "bedrock_addon_auto_loader"), new BedrockAddonAutoLoader());
    }
    //?}

    private interface ReloadListenerRegistrar {
        void register(String key, PreparableReloadListener listener);
    }

    private static void registerAnnotatedListeners(ReloadListenerRegistrar registrar) {
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

                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    PreparableReloadListener instance = (PreparableReloadListener) constructor.newInstance();
                    registrar.register(clazz.getSimpleName().toLowerCase(java.util.Locale.ROOT), instance);
                } catch (Exception | LinkageError e) {
                    LOGGER.error("[ResourceLoader] Failed to load: {}", memberName, e);
                }
            }
        }
    }
}
