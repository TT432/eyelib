package io.github.tt432.eyelib;

import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;

@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        EyelibNetworkManager.register();
    }
}
