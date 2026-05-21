package io.github.tt432.eyelibattachment.sync;

import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;

/**
 * @author TT432
 */
/** @author TT432 */
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