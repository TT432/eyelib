package io.github.tt432.eyelib;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.client.loader.BrClientEntityLoader;
import io.github.tt432.eyelib.client.manager.*;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        EyelibAttachableData.DATA_ATTACHMENTS.register(bus);

        EyelibNetworkManager.register();
    }

    public static RenderHelper getRenderHelper() {
        return RenderHelper.start();
    }

    public static AnimationManager getAnimationManager() {
        return AnimationManager.INSTANCE;
    }

    public static MaterialManager getMaterialManager() {
        return MaterialManager.INSTANCE;
    }

    public static ModelManager getModelManager() {
        return ModelManager.INSTANCE;
    }

    public static RenderControllerManager getRenderControllerManager() {
        return RenderControllerManager.INSTANCE;
    }

    public static ParticleManager getParticleManager() {
        return ParticleManager.INSTANCE;
    }

    public static BrClientEntityLoader getClientEntityLoader() {
        return BrClientEntityLoader.INSTANCE;
    }
}
