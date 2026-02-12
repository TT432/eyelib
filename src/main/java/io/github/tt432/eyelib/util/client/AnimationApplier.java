package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@UtilityClass
public class AnimationApplier {
    public <M extends Model.Bone<M>, R extends ModelRuntimeData<M>> void apply(
            BoneRenderInfoEntry entry, M model, R data
    ) {
        var initPosition = data.initPosition(model);
        Vector3f renderPosition = entry.getRenderPosition();
        data.position(
                model,
                renderPosition.x + initPosition.x(),
                renderPosition.y + initPosition.y(),
                renderPosition.z + initPosition.z()
        );
        var initRotation = data.initRotation(model);
        Vector3f renderRotation = entry.getRenderRotation();
        data.rotation(
                model,
                renderRotation.x + initRotation.x(),
                renderRotation.y + initRotation.y(),
                renderRotation.z + initRotation.z()
        );
        var initScale = data.initScale(model);
        Vector3f renderScala = entry.getRenderScala();
        data.scale(
                model,
                renderScala.x * initScale.x(),
                renderScala.y * initScale.y(),
                renderScala.z * initScale.z()
        );
    }
}
