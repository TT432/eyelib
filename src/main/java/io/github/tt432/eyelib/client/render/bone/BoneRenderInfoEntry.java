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
    Vector3f renderPivot;
    @NotNull
    Vector3f renderRotation;

    public BoneRenderInfoEntry(BrBone bindBone) {
        this.bindBone = bindBone;
        renderPivot = new Vector3f(bindBone.pivot());
        renderRotation = new Vector3f(bindBone.rotation());
    }

    public void resetRenderInfo() {
        renderScala.set(1);
        renderPivot.set(bindBone.pivot());
        renderRotation.set(bindBone.rotation());
    }
}
