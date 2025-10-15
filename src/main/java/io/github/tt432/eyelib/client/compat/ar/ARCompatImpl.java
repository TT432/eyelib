package io.github.tt432.eyelib.client.compat.ar;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.render.RenderParams;
import lombok.experimental.ExtensionMethod;

@ExtensionMethod(VertexConsumerExtension.class)
public class ARCompatImpl {
    public static boolean renderWithAR(BakedModel.BakedBone bakedBone, RenderParams params) {
        var extension = params.consumer().getAccelerated();
        var pose = params.poseStack().last();

        if (AcceleratedEntityRenderingFeature.isEnabled()
                && AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                && (CoreFeature.isRenderingLevel() || (CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui()))
                && extension.isAccelerated()
        ) {
            extension.doRender(
                    AcceleratedBakedBoneRenderer.INSTANCE,
                    bakedBone,
                    pose.pose(),
                    pose.normal(),
                    params.light(),
                    params.overlay(),
                    -1
            );

            return true;
        } else {
            return false;
        }
    }
}
