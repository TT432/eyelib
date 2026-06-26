package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.render.bake.EmissiveModelBakeInfo;
import io.github.tt432.eyelib.client.render.bake.TwoSideModelBakeInfo;

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

        ManagerEntryChangedEventPublisher.addListener(event -> {
            if (!ModelManager.class.getSimpleName().equals(event.getManagerName())) {
                return;
            }

            EmissiveModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
            TwoSideModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName());
        });
    }
}
