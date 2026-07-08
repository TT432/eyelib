package io.github.tt432.eyelib.client.render.visitor;
import io.github.tt432.eyelib.model.ModelVisitContext;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.compat.ar.ARCompat;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.Model;
/**
 * @author TT432
 */
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
            if (ARCompat.renderWithAR(
                    renderParams.consumer(), renderParams.poseStack().last(),
                    bakedBone.position(), bakedBone.u(), bakedBone.v(), bakedBone.normal(),
                    bakedBone.vertexSize(), renderParams.light(), renderParams.overlay()
            )) {
                return;
            }
            renderBakedBone(renderParams, bakedBone);
        }
    }
}
