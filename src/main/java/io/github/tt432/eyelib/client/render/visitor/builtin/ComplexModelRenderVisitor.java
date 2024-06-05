package io.github.tt432.eyelib.client.render.visitor.builtin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

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
    public void visitBone(@Nullable Entity renderTarget, PoseStack poseStack, BrBone bone, RenderType renderType, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer, boolean before) {
        visitorA.visitBone(renderTarget, poseStack, bone, renderType, boneRenderInfoEntry, consumer, before);
        visitorB.visitBone(renderTarget, poseStack, bone, renderType, boneRenderInfoEntry, consumer, before);
    }

    @Override
    public void visitCube(@Nullable Entity renderTarget, PoseStack poseStack, BrCube cube, RenderType renderType, VertexConsumer consumer) {
        visitorA.visitCube(renderTarget, poseStack, cube, renderType, consumer);
        visitorB.visitCube(renderTarget, poseStack, cube, renderType, consumer);
    }

    @Override
    public void visitVertex(@Nullable Entity renderTarget, PoseStack poseStack, BrCube cube, RenderType renderType, BrFace face, int vertexId, VertexConsumer consumer) {
        visitorA.visitVertex(renderTarget, poseStack, cube, renderType, face, vertexId, consumer);
        visitorB.visitVertex(renderTarget, poseStack, cube, renderType, face, vertexId, consumer);
    }

    @Override
    public void visitLocator(@Nullable Entity renderTarget, PoseStack poseStack, BrBone bone, RenderType renderType, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry, VertexConsumer consumer) {
        visitorA.visitLocator(renderTarget, poseStack, bone, renderType, name, locator, boneRenderInfoEntry, consumer);
        visitorB.visitLocator(renderTarget, poseStack, bone, renderType, name, locator, boneRenderInfoEntry, consumer);
    }
}
