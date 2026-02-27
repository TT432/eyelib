package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@UtilityClass
public class AnimationApplier {
    public void apply(ModelRuntimeData.Entry entry, Model.Bone model, ModelRuntimeData data) {
        var initPosition = model.position();
        Vector3f renderPosition = entry.position;
        data.position(
                model,
                renderPosition.x + initPosition.x(),
                renderPosition.y + initPosition.y(),
                renderPosition.z + initPosition.z()
        );
        var initRotation = model.rotation();
        Vector3f renderRotation = entry.rotation;
        data.rotation(
                model,
                renderRotation.x + initRotation.x(),
                renderRotation.y + initRotation.y(),
                renderRotation.z + initRotation.z()
        );
        var initScale = model.scale();
        Vector3f renderScala = entry.scale;
        data.scale(
                model,
                renderScala.x * initScale.x(),
                renderScala.y * initScale.y(),
                renderScala.z * initScale.z()
        );
    }
}
