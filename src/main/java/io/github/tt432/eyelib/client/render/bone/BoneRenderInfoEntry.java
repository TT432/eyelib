package io.github.tt432.eyelib.client.render.bone;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@Data
public class BoneRenderInfoEntry {
    private final BrBone bindBone;

    @NotNull
    Vector3f renderScala = new Vector3f(1);
    @NotNull
    Vector3f renderPosition;
    @NotNull
    Vector3f renderRotation;

    public BoneRenderInfoEntry(BrBone bindBone) {
        this.bindBone = bindBone;
        renderPosition = new Vector3f();
        renderRotation = new Vector3f(bindBone.rotation());
    }

    public void resetRenderInfo() {
        renderScala.set(1);
        renderPosition.set(0);
        renderRotation.set(bindBone.rotation());
    }
}
