package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class CollectLocatorModelVisitor extends ModelVisitor {
    @Override
    public <R extends ModelRuntimeData<Model.Bone, ?, R>> void visitLocator(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, LocatorEntry locator, R data, ModelTransformer<Model.Bone, R> transformer) {
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
