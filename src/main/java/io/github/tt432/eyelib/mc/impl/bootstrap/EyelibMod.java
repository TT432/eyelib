package io.github.tt432.eyelib.mc.impl.bootstrap;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;

@Mod(Eyelib.MOD_ID)
public class EyelibMod {
    public EyelibMod() {
        EyelibNetworkManager.register();
    }
}
