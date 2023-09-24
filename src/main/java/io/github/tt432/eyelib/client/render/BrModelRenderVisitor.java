package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;

/**
 * @author TT432
 * @see BrModelRenderer
 */
public class BrModelRenderVisitor {

    public void visitModel(PoseStack poseStack, BrModel model, VertexConsumer consumer) {

    }

    public void visitBone(PoseStack poseStack, BrBone bone, VertexConsumer consumer, boolean before) {

    }

    public void visitCube(PoseStack poseStack, BrCube cube, VertexConsumer consumer) {

    }

    public void visitVertex(PoseStack poseStack, BrCube cube, BrFace face, int vertexId, VertexConsumer consumer) {

    }

}
