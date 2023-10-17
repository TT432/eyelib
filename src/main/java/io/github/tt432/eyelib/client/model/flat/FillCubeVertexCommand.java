package io.github.tt432.eyelib.client.model.flat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;

/**
 * @author TT432
 */
public class FillCubeVertexCommand implements FlatBrModelCommand {
    final BrCube cube;

    public FillCubeVertexCommand(BrCube cube) {
        this.cube = cube;
    }

    @Override
    public void doCommand(PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        visitor.visitCube(poseStack, cube, consumer);

        for (BrFace face : cube.faces()) {
            for (int i = 0; i < face.getVertex().length; i++) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }

            for (int i = face.getVertex().length - 1; i >= 0; i--) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }
        }
    }
}
