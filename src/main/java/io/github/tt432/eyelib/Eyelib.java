package io.github.tt432.eyelib;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib(IEventBus bus) {
        EyelibAttachableData.ATTACHMENT_TYPES.register(bus);
        BuiltInBrModelRenderVisitors.VISITORS.register(bus);
    }
}
