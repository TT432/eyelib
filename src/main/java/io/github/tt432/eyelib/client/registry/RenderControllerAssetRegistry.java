package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderControllerAssetRegistry {
    public static void replaceRenderControllers(Map<?, RenderControllers> controllers) {
        LinkedHashMap<String, RenderControllerEntry> flattened = new LinkedHashMap<>();
        for (RenderControllers value : controllers.values()) {
            value.render_controllers().forEach(flattened::put);
        }
        RenderControllerManager.writePort().replaceAll(flattened);
    }

    public static void publishRenderController(RenderControllers controller) {
        controller.render_controllers().forEach(RenderControllerManager.writePort()::put);
    }
}
