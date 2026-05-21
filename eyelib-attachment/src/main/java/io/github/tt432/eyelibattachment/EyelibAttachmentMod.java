package io.github.tt432.eyelibattachment;

import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author TT432
 */
@Mod(EyelibAttachmentMod.MOD_ID)
/** @author TT432 */
public class EyelibAttachmentMod {
    public static final String MOD_ID = "eyelibattachment";

    public EyelibAttachmentMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
    }
}