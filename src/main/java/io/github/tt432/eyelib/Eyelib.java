package io.github.tt432.eyelib;

import io.github.tt432.eyelib.capability.EyelibCapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib(IEventBus bus) {
        EyelibCapabilities.ATTACHMENT_TYPES.register(bus);
    }
}
