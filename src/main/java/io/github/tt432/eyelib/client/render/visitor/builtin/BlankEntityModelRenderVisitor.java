package io.github.tt432.eyelib.client.render.visitor.builtin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import lombok.Setter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * @author TT432
 */
@Setter
public class BlankEntityModelRenderVisitor extends ModelRenderVisitor {
    private static final Vector4f tPosition = new Vector4f();
    private static final Vector3f tNormal = new Vector3f();

    int light;

    @Override
    public void setupLight(int light) {
        this.light = light;
    }

    @Override
    public void visitVertex(PoseStack poseStack, BrCube cube, RenderType renderType, BrFace face, int vertexId, VertexConsumer consumer) {
        Vector3f normal = face.getNormal();
        Vector3f vertex = face.getVertex()[vertexId];
        Vector2f uv = face.getUv()[vertexId];

        PoseStack.Pose last = poseStack.last();

        last.pose().transformAffine(vertex.x, vertex.y, vertex.z, 1, tPosition);
        last.normal().transform(normal, tNormal);

        consumer.vertex(tPosition.x, tPosition.y, tPosition.z,
                1, 1, 1, 1,
                uv.x, uv.y,
                OverlayTexture.NO_OVERLAY, light,
                tNormal.x, tNormal.y, tNormal.z);
    }
}
