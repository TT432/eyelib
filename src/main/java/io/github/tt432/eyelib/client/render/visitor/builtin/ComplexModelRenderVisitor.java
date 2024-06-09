package io.github.tt432.eyelib.client.render.visitor.builtin;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.RenderParams;
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
    public void visitBone(RenderParams renderParams, BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry, boolean before) {
        visitorA.visitBone(renderParams, bone, boneRenderInfoEntry, before);
        visitorB.visitBone(renderParams, bone, boneRenderInfoEntry, before);
    }

    @Override
    public void visitCube(RenderParams renderParams, BrCube cube) {
        visitorA.visitCube(renderParams, cube);
        visitorB.visitCube(renderParams, cube);
    }

    @Override
    public void visitVertex(RenderParams renderParams, BrCube cube, BrFace face, int vertexId) {
        visitorA.visitVertex(renderParams, cube, face, vertexId);
        visitorB.visitVertex(renderParams, cube, face, vertexId);
    }

    @Override
    public void visitLocator(RenderParams renderParams, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry) {
        visitorA.visitLocator(renderParams, bone, name, locator, boneRenderInfoEntry);
        visitorB.visitLocator(renderParams, bone, name, locator, boneRenderInfoEntry);
    }
}
