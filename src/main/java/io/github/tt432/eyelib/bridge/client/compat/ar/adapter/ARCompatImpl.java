//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.compat.ar.adapter;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.ExtensionMethod;
import org.jspecify.annotations.Nullable;
/**
 * 加速渲染（AR）兼容性实现的工具类。
 * 提供条件性加速渲染的调用入口，仅在加速管线可用时执行。
 *
 * @author TT432
 */
@ExtensionMethod(VertexConsumerExtension.class)
public final class ARCompatImpl {
    private ARCompatImpl() {}

    public static boolean renderWithAR(
            @Nullable VertexConsumer consumer, PoseStack.Pose pose,
            float[] positions, float[] u, float[] v, float[] normals,
            int vertexSize, int light, int overlay) {
        if (consumer == null) {
            return false;
        }

        var extension = consumer.getAccelerated();

        if (AcceleratedEntityRenderingFeature.isEnabled()
                && AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                && (CoreFeature.isRenderingLevel() || (CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui()))
                && extension.isAccelerated()
        ) {
            ARBoneData data = new ARBoneData(positions, u, v, normals, vertexSize);

            extension.doRender(
                    AcceleratedBakedBoneRenderer.INSTANCE,
                    data,
                    pose.pose(),
                    pose.normal(),
                    light,
                    overlay,
                    -1
            );

            return true;
        } else {
            return false;
        }
    }
}
//?}
