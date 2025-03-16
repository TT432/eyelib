package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.render.RenderParams;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * @author TT432
 */
public class RenderModelVisitor extends ModelVisitor {
    private static final Vector4f tPosition = new Vector4f();
    private static final Vector3f tNormal = new Vector3f();

    @Override
    public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube, Vector3fc vertex, Vector2fc uv, Vector3fc normal) {
        PoseStack poseStack = renderParams.poseStack();
        PoseStack.Pose last = poseStack.last();

        last.pose().transformAffine(vertex.x(), vertex.y(), vertex.z(), 1, tPosition);
        last.normal().transform(normal, tNormal);

        VertexConsumer consumer = renderParams.consumer();
        consumer.addVertex(tPosition.x, tPosition.y, tPosition.z,
                0xFF_FF_FF_FF,
                uv.x(), uv.y(),
                renderParams.overlay(), renderParams.light(),
                tNormal.x, tNormal.y, tNormal.z);
    }
}
