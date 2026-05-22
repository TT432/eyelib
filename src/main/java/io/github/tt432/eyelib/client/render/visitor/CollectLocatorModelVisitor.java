package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibmodel.locator.LocatorEntry;
import io.github.tt432.eyelib.client.render.RenderParams;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public class CollectLocatorModelVisitor extends ModelVisitor {
    @Override
    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {

    }

    @Override
    public void visitLocator(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, LocatorEntry locator, ModelRuntimeData data) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();

        pose.translate(locator.offset());

        var rotation = locator.rotation();

        last.normal().rotateZYX(rotation.z(), rotation.y(), rotation.x());
        last.pose().rotateZYX(rotation.z(), rotation.y(), rotation.x());

        context.<Map<String, Matrix4f>>orCreate("locators", new HashMap<>()).put(locator.name(), new Matrix4f(poseStack.poseStack.getLast().pose()));
        poseStack.popPose();
    }
}