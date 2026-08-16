package io.github.tt432.eyelib.bridge;

import io.github.tt432.eyelib.bridge.network.adapter.EyelibNetworkTransport;

import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.animation.AnimationLocatorResolver;
import io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter;
//? if <1.20.6 {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//?}

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    //? if <1.20.6 {
    public Eyelib() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
    //?} else {
    public Eyelib(IEventBus bus) {
    //?}

        try {
            Class.forName("io.github.tt432.eyelib.capability.AttachableDataTypes");
            Class<?> implClass = Class.forName("io.github.tt432.eyelib.client.lifecycle.ApplicationLifecyclePortImpl");
            var lifecycleConstructor = implClass.getDeclaredConstructor();
            lifecycleConstructor.setAccessible(true);
            ApplicationLifecyclePort.install((ApplicationLifecyclePort) lifecycleConstructor.newInstance());
            //? if >=1.20.6 {
            Class.forName("io.github.tt432.eyelib.bridge.capability.EyelibAttachableData");
            //?}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
        AnimationLocatorResolver.install();
        EntityPortAdapter.installArrowHostBridge();
        //? if >=1.20.6
        DataAttachmentContainerCapability.register(bus);
        //? if <26.1
        bus.addListener(io.github.tt432.eyelib.bridge.client.sound.adapter.AddonSoundBridge::onAddPackFinders);
        bus.addListener(io.github.tt432.eyelib.bridge.client.loader.adapter.BedrockAddonPackFinder::onAddPackFinders);
        //? if <1.20.6 {
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.registerNetworkHandlers();
        //?} else {
        bus.addListener(io.github.tt432.eyelib.bridge.network.adapter.EyelibNetworkTransport::onRegisterPayloads);
        //?}

        if (Boolean.getBoolean("eyelib.benchmark.enabled")) {
            try {
                Class.forName("io.github.tt432.eyelib.debug.benchmark.ClientBenchmarkRunner")
                        .getMethod("install")
                        .invoke(null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                Throwable root = cause == null ? e : cause;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                throw new IllegalStateException("Failed to install Eyelib benchmark runner: " + root, root);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to install Eyelib benchmark runner", e);
            }
        }
    }
}



