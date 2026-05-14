package io.github.tt432.eyelib.client.render.sync;

import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;

/**
 * Platform-type-free model sync payload used by the render apply seam.
 */
public record RenderModelSyncPayload(
        String model,
        String texture,
        String renderType
) {
    public static RenderModelSyncPayload from(ModelComponentInfo info) {
        return new RenderModelSyncPayload(
                info.model(),
                info.texture().toString(),
                info.renderType().toString()
        );
    }
}
