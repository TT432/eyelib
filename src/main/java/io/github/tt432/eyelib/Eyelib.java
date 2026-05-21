package io.github.tt432.eyelib;

import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NullMarked;

@Mod(Eyelib.MOD_ID)

/** @author TT432 */
@NullMarked
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
