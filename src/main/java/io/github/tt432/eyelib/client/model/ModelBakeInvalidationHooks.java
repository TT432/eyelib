package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.bridge.event.ManagerEventPort;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.bridge.event.ManagerReplacedEventPublisher;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;

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

        ManagerEntryChangedEventPublisher.<ManagerEventPort>addListener(event -> {
            if (!ModelManager.class.getSimpleName().equals(event.getManagerName())) {
                return;
            }

            ModelBakePort.twoSideInvalidateModel(event.getEntryName());
        });

        // 批量替换（replaceAll/putAll）无逐条事件：烘焙缓存整体失效。
        ManagerReplacedEventPublisher.addListener(managerName -> {
            if (!ModelManager.class.getSimpleName().equals(managerName)) {
                return;
            }

            ModelBakePort.twoSideInvalidateAll();
        });
    }
}

