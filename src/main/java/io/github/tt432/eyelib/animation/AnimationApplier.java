package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;
/**
 * 将动画变换（位置/旋转/缩放）应用到模型骨骼上。
 *
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