package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.render.PoseStackPort;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoseCopies {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return PoseStackPort.copy(pose);
    }
}
