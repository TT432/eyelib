package io.github.tt432.eyelib;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
@Slf4j
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        EyelibAttachableData.DATA_ATTACHMENTS.register(bus);

        EyelibNetworkManager.register();

//        new ModBridgeServer(19132, json -> Minecraft.getInstance().submit(() -> {
//            log.debug("ModBridge update: size={} bytes", json.length());
//            MinecraftForge.EVENT_BUS.post(new ModBridgeModelUpdateEvent(json));
//        })).start();
    }

    public static RenderHelper getRenderHelper() {
        return RenderHelper.start();
    }
}
