package io.github.tt432.eyelib.mc.impl.bootstrap;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge composition root for Eyelib mod startup.
 *
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class EyelibMod {
    public EyelibMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        EyelibAttachableData.DATA_ATTACHMENTS.register(bus);

        EyelibNetworkManager.register();
    }
}
