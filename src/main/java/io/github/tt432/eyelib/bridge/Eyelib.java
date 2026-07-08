package io.github.tt432.eyelib.bridge;

import io.github.tt432.eyelib.bridge.network.adapter.EyelibNetworkTransport;

import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.animation.AnimationLocatorResolver;
//? if <1.20.6 {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
//?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
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
        //? if >=1.20.6
        DataAttachmentContainerCapability.register(bus);
        //? if <1.20.6 {
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.registerNetworkHandlers();
        //?} else {
        bus.addListener(io.github.tt432.eyelib.bridge.network.adapter.EyelibNetworkTransport::onRegisterPayloads);
        //?}

        if (!(//? if <26.1 {
            FMLLoader.isProduction()
            //?} else {
            false
            //?}
        )) {
            try {
                Class<?> serverClass = Class.forName("io.github.tt432.eyelib.common.debug.AIDebugServer");
                Object server = serverClass.getDeclaredConstructor().newInstance();
                serverClass.getMethod("start").invoke(server);
            } catch (Exception ignored) {
            }
        }
    }
}



