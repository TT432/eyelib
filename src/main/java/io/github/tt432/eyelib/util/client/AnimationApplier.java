package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@UtilityClass
public class AnimationApplier {
    public <M extends Model.Bone, R extends ModelRuntimeData<M, ?, R>> void apply(
            BoneRenderInfoEntry entry, M model, R data, ModelTransformer<M, R> transformer
    ) {
        var initPosition = transformer.initPosition(model, data);
        Vector3f renderPosition = entry.getRenderPosition();
        transformer.position(
                model,
                data,
                renderPosition.x + initPosition.x(),
                renderPosition.y + initPosition.y(),
                renderPosition.z + initPosition.z()
        );
        var initRotation = transformer.initRotation(model, data);
        Vector3f renderRotation = entry.getRenderRotation();
        transformer.rotation(
                model,
                data,
                renderRotation.x + initRotation.x(),
                renderRotation.y + initRotation.y(),
                renderRotation.z + initRotation.z()
        );
        var initScale = transformer.initScale(model, data);
        Vector3f renderScala = entry.getRenderScala();
        transformer.scale(
                model,
                data,
                renderScala.x * initScale.x(),
                renderScala.y * initScale.y(),
                renderScala.z * initScale.z()
        );
    }
}
