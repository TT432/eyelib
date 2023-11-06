package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static io.github.tt432.eyelib.client.render.PoolHandler.*;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrModelRenderer {
    private static final Vector3f nPivot = new Vector3f();
    private static final Quaternionf tQ = new Quaternionf();

    public static void render(BrModel model, PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        PoseStack.Pose last = poseStack.last();

        try {
            m3l.addLast(m3f.borrowObject().set(last.normal()));
            m4l.addLast(m4f.borrowObject().set(last.pose()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (BrBone toplevelBone : model.toplevelBones()) {
            renderBone(visitor, toplevelBone, consumer);
        }

        pop();
    }

    static void push() {
        try {
            m3l.addLast(m3f.borrowObject().set(m3l.getFirst()));
            m4l.addLast(m4f.borrowObject().set(m4l.getFirst()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void pop() {
        m3f.returnObject(m3l.pollLast());
        m4f.returnObject(m4l.pollLast());
    }

    private static void renderBone(BrModelRenderVisitor visitor, BrBone bone, VertexConsumer consumer) {
        push();

        var m3 = m3l.getLast();
        var m4 = m4l.getLast();

        visitor.visitBone(m3, m4, bone, consumer, true);

        Vector3f renderPivot = bone.getRenderPivot();

        if (renderPivot == null) {
            renderPivot = bone.getPivot();
        }

        m4.translate(renderPivot);

        Vector3f rotation = bone.getRenderRotation();

        if (rotation == null) {
            rotation = bone.getRotation();
        }

        tQ.rotationZYX(rotation.z, rotation.y, rotation.x);
        m3.rotate(tQ);
        m4.rotate(tQ);

        m4.translate(renderPivot.negate(nPivot));

        Vector3f scale = bone.getRenderScala();

        m4.scale(scale.x, scale.y, scale.z);
        if (scale.x == scale.y && scale.y == scale.z && scale.x <= 0.0F) {
            m3.scale(-1.0F);
        }

        float f = 1.0F / scale.x;
        float f1 = 1.0F / scale.y;
        float f2 = 1.0F / scale.z;
        float f3 = Mth.fastInvCubeRoot(f * f1 * f2);
        m3.scale(f3 * f, f3 * f1, f3 * f2);

        visitor.visitBone(m3, m4, bone, consumer, false);

        for (BrCube cube : bone.getCubes()) {
            renderCube(m3, m4, visitor, cube, consumer);
        }

        for (BrBone child : bone.getChildren()) {
            renderBone(visitor, child, consumer);
        }

        pop();
    }

    private static void renderCube(Matrix3f m3, Matrix4f m4, BrModelRenderVisitor visitor, BrCube cube, VertexConsumer consumer) {
        visitor.visitCube(m3, m4, cube, consumer);

        for (BrFace face : cube.faces()) {
            for (int i = 0; i < face.getVertex().length; i++) {
                visitor.visitVertex(m3, m4, cube, face, i, consumer);
            }

            for (int i = face.getVertex().length - 1; i >= 0; i--) {
                visitor.visitVertex(m3, m4, cube, face, i, consumer);
            }
        }
    }
}
