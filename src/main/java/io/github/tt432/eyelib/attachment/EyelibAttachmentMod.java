package io.github.tt432.eyelib.attachment;

import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 附加数据模块的主模组类。
 *
 * @author TT432
 */
@Mod(EyelibAttachmentMod.MOD_ID)
public class EyelibAttachmentMod {
    public static final String MOD_ID = "eyelibattachment";

    public EyelibAttachmentMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
    }
}