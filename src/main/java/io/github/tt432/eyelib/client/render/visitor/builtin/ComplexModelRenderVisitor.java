package io.github.tt432.eyelib.client.render.visitor.builtin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import lombok.AllArgsConstructor;

/**
 * @author TT432
 */
@AllArgsConstructor
public class ComplexModelRenderVisitor extends ModelRenderVisitor {
    public final ModelRenderVisitor visitorA;
    public final ModelRenderVisitor visitorB;

    @Override
    public void setupLight(int light) {
        visitorA.setupLight(light);
        visitorB.setupLight(light);
    }

    @Override
    public void visitBone(PoseStack poseStack, BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer, boolean before) {
        visitorA.visitBone(poseStack, bone, boneRenderInfoEntry, consumer, before);
        visitorB.visitBone(poseStack, bone, boneRenderInfoEntry, consumer, before);
    }

    @Override
    public void visitCube(PoseStack poseStack, BrCube cube, VertexConsumer consumer) {
        visitorA.visitCube(poseStack, cube, consumer);
        visitorB.visitCube(poseStack, cube, consumer);
    }

    @Override
    public void visitVertex(PoseStack poseStack, BrCube cube, BrFace face, int vertexId, VertexConsumer consumer) {
        visitorA.visitVertex(poseStack, cube, face, vertexId, consumer);
        visitorB.visitVertex(poseStack, cube, face, vertexId, consumer);
    }

    @Override
    public void visitLocator(PoseStack poseStack, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer) {
        visitorA.visitLocator(poseStack, bone, name, locator, boneRenderInfoEntry, consumer);
        visitorB.visitLocator(poseStack, bone, name, locator, boneRenderInfoEntry, consumer);
    }
}
