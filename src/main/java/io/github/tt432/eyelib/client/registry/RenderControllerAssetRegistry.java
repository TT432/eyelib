package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;


/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class RenderControllerAssetRegistry {

    public static void replaceRenderControllers(Map<?, RenderControllers> controllers) {
        for (RenderControllers value : controllers.values()) {
            value.render_controllers().forEach((key, entry) -> {
                RenderControllerEntry existing = RenderControllerManager.readPort().get(key);
                if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                    return;
                }
                RenderControllerManager.writePort().put(key, entry);
            });
        }
    }

    public static void publishRenderController(RenderControllers controller) {
        controller.render_controllers().forEach((key, entry) -> {
            RenderControllerEntry existing = RenderControllerManager.readPort().get(key);
            if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                return;
            }
            RenderControllerManager.writePort().put(key, entry);
        });
    }
}
