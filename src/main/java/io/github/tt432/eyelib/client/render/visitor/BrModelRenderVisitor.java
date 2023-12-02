package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;

/**
 * @author TT432
 */
public class BrModelRenderVisitor {

    public void setupLight(int light) {

    }

    public void visitBone(PoseStack poseStack, BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer, boolean before) {

    }

    public void visitCube(PoseStack poseStack, BrCube cube, VertexConsumer consumer) {

    }

    public void visitVertex(PoseStack poseStack, BrCube cube, BrFace face, int vertexId, VertexConsumer consumer) {

    }

    public void visitLocator(PoseStack poseStack, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer) {

    }
}
