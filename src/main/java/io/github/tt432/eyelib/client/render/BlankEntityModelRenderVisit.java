package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import lombok.Setter;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public class BlankEntityModelRenderVisit extends BrModelRenderVisitor {
    @Setter
    int light;

    @Override
    public void setupLight(int light) {
        this.light = light;
    }

    @Override
    public void visitVertex(PoseStack poseStack, BrCube cube,
                            BrFace face, int vertexId, VertexConsumer consumer) {
        Vector3f normal = face.getNormal();
        Vector3f vertex = face.getVertex()[vertexId];
        Vector2f uv = face.getUv()[vertexId];
        consumer.vertex(poseStack.last().pose(), vertex.x, vertex.y, vertex.z)
                .color(0xFF_FF_FF_FF)
                .uv(uv.x, uv.y)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), normal.x, normal.y, normal.z)
                .endVertex();
    }
}
