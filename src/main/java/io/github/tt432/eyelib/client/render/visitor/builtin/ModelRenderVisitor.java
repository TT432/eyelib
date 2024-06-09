package io.github.tt432.eyelib.client.render.visitor.builtin;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;

/**
 * @author TT432
 */
public class ModelRenderVisitor {

    public void visitBone(RenderParams renderParams, BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry, boolean before) {
        // need child to impl this
    }

    public void visitCube(RenderParams renderParams, BrCube cube) {
        // need child to impl this
    }

    public void visitVertex(RenderParams renderParams, BrCube cube, BrFace face, int vertexId) {
        // need child to impl this
    }

    public void visitLocator(RenderParams renderParams, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry) {
        // need child to impl this
    }
}
