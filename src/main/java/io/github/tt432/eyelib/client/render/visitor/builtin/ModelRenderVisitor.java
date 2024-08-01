package io.github.tt432.eyelib.client.render.visitor.builtin;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;

/**
 * @author TT432
 * @see io.github.tt432.eyelib.client.render.renderer.BrModelRenderer
 */
public class ModelRenderVisitor {
    public void visitModel(RenderParams renderParams) {

    }

    public void visitBone(RenderParams renderParams, BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry) {

    }

    public void visitCube(RenderParams renderParams, BrCube cube) {

    }

    public void visitFace(RenderParams renderParams, BrCube cube, BrFace face) {

    }

    public void visitVertex(RenderParams renderParams, BrCube cube, BrFace face, int vertexId) {

    }

    public void visitLocator(RenderParams renderParams, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry) {

    }
}
