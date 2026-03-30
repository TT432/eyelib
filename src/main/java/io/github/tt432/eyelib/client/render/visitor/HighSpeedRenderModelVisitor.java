package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.compat.ar.ARCompat;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.render.RenderParams;
import lombok.Setter;

/**
 * @author TT432
 */
@Setter
public class HighSpeedRenderModelVisitor extends ModelVisitor {
    @Override
    public  void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
        super.visitPreModel(params, context, infos, model);

        if (!context.contains("BackedModel")) {
            throw new RuntimeException("can't use HighSpeedRenderModelVisitor without HBackedModel");
        }
    }

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

        PoseStack.Pose last = poseStack.last();

        if (renderParams.partVisibility().getOrDefault(bone.id(), true)) {
            if (ARCompat.AR_INSTALLED && ARCompat.renderWithAR(bakedBone, renderParams)) {
                return;
            }

             bakedBone.transformPos(last.pose());
             bakedBone.transformNormal(last.normal());

             if (renderParams.consumer() != null) {
                 visitVertex(bakedBone, renderParams.consumer(), renderParams.overlay(), renderParams.light());
             }
        }
    }

    @Override
    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
    }

    private static void visitVertex(BakedModel.BakedBone bakedBone, VertexConsumer consumer, int overlay, int light) {
        for (int nIdx = 0; nIdx < bakedBone.vertexSize(); nIdx++) {
            consumer.vertex(
                    bakedBone.positionResult()[nIdx * 3],
                    bakedBone.positionResult()[nIdx * 3 + 1],
                    bakedBone.positionResult()[nIdx * 3 + 2],
                    1, 1, 1, 1,
                    bakedBone.u()[nIdx], bakedBone.v()[nIdx], overlay, light,
                    bakedBone.normalResult()[nIdx * 3],
                    bakedBone.normalResult()[nIdx * 3 + 1],
                    bakedBone.normalResult()[nIdx * 3 + 2]
            );
        }
    }
}
