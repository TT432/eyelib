package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * @author TT432
 */
public class BrModelRenderVisitor {

    public void setupLight(int light) {

    }

    public void visitBone(Matrix3f normal, Matrix4f pose, BrBone bone, VertexConsumer consumer, boolean before) {

    }

    public void visitCube(Matrix3f m3, Matrix4f m4, BrCube cube, VertexConsumer consumer) {

    }

    public void visitVertex(Matrix3f m3, Matrix4f m4, BrCube cube, BrFace face, int vertexId, VertexConsumer consumer) {

    }

}
