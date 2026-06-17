package io.github.tt432.eyelib;

import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
@NullMarked
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);

        EyelibNetworkManager.register();
        if (!FMLLoader.isProduction()) {
            try {
                Class<?> serverClass = Class.forName("io.github.tt432.eyelib.common.debug.AIDebugServer");
                Object server = serverClass.getDeclaredConstructor().newInstance();
                serverClass.getMethod("start").invoke(server);
            } catch (Exception ignored) {
            }
        }
    }
}
