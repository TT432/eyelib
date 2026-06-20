package io.github.tt432.eyelib;

import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.animation.AnimationLocatorResolver;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
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

        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
        AnimationLocatorResolver.install();
        //? if >=1.20.6
        DataAttachmentContainerCapability.register(bus);
        //? if <1.20.6 {
        EyelibNetworkManager.register();
        //?} else {
        bus.addListener(io.github.tt432.eyelib.network.EyelibNetworkTransport::onRegisterPayloads);
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
