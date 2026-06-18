package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.render.bake.EmissiveModelBakeInfo;
import io.github.tt432.eyelib.client.render.bake.TwoSideModelBakeInfo;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
//? if <1.20.6 {
import net.minecraftforge.common.MinecraftForge;
//?} else {
import net.neoforged.neoforge.common.NeoForge;
//?}
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author TT432
 */
public final class ModelBakeInvalidationHooks {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private ModelBakeInvalidationHooks() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        //? if <1.20.6 {
        MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(event -> {
        //?} else {
        NeoForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(event -> {
        //?}
            if (!ModelManager.class.getSimpleName().equals(event.getManagerName())) {
                return;
            }

            EmissiveModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
            TwoSideModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
        });
    }
}
