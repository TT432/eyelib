package io.github.tt432.eyelib.attachment.sync;

import io.github.tt432.eyelib.attachment.capability.ModelComponentInfo;
import io.github.tt432.eyelib.util.PortResourceLocation;

/**
 * @author TT432
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

    public ModelComponentInfo toInfo() {
        return new ModelComponentInfo(
                model(),
                PortResourceLocation.parse(texture()),
                PortResourceLocation.parse(renderType())
        );
    }
}