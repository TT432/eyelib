package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.client.render.bake.EmissiveModelBakeInfo;
import io.github.tt432.eyelib.client.render.bake.TwoSideModelBakeInfo;
import net.minecraftforge.common.MinecraftForge;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;

/** @author TT432 */
@NullMarked
public final class ModelBakeInvalidationHooks {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private ModelBakeInvalidationHooks() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(event -> {
            if (!ModelManager.class.getSimpleName().equals(event.getManagerName())) {
                return;
            }

            EmissiveModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
            TwoSideModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
        });
    }
}
