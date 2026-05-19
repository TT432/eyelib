package io.github.tt432.eyelib;

import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.common.Mod;

@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
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
