package io.github.tt432.eyelib.client.render.bone;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@Data
public class BoneRenderInfoEntry {
    @NotNull
    private final Vector3f renderScala = new Vector3f(1);
    @NotNull
    private final Vector3f renderPosition = new Vector3f();
    @NotNull
    private final Vector3f renderRotation = new Vector3f();

    public void resetRenderInfo() {
        renderScala.set(1);
        renderPosition.set(0);
        renderRotation.set(0);
    }
}
