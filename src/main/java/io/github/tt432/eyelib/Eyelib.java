package io.github.tt432.eyelib;

import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BuiltInBrModelRenderVisitors.VISITORS.register(bus);
        EyelibNetworkManager.register();
    }
}
