package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.compat.ar.ARCompat;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bake.BakedModel;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibmodel.Model;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public class ARBakedVisitor extends HighSpeedRenderModelVisitor {
    @Override
    public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(context, poseStack, bone, cast(data));

        var bakedModel = context.<BakedModel>get("BackedModel");
        if (bakedModel == null) {
            return;
        }
        var bakedBone = bakedModel.bones().get(bone.id());
        if (bakedBone == null) {
            return;
        }

        if (renderParams.partVisibility().getOrDefault(bone.id(), true)) {
            if (ARCompat.renderWithAR(bakedBone, renderParams)) {
                return;
            }
            renderBakedBone(renderParams, bakedBone);
        }
    }
}