package io.github.tt432.eyelib.client.render.visitor;
import io.github.tt432.eyelib.model.ModelVisitContext;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.client.render.VertexConsumerPort;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.Model;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
@Setter
public class HighSpeedRenderModelVisitor extends ModelVisitor {
    @Override
    public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
        super.visitPreModel(params, context, infos, model);

        if (!context.contains("BackedModel")) {
            throw new RuntimeException("can't use HighSpeedRenderModelVisitor without BackedModel");
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

        if (renderParams.partVisibility().getOrDefault(bone.id(), true)) {
            renderBakedBone(renderParams, bakedBone);
        }
    }

    protected void renderBakedBone(RenderParams renderParams, BakedModel.BakedBone bakedBone) {
        PoseStack.Pose last = renderParams.poseStack().last();
        bakedBone.transformPos(last.pose());
        bakedBone.transformNormal(last.normal());

        if (renderParams.consumer() != null) {
            float @Nullable [] tint = renderParams.tintColor();
            visitVertex(bakedBone, renderParams.consumer(), renderParams.overlay(), renderParams.light(), tint);
        }
    }

    static void visitVertex(BakedModel.BakedBone bakedBone, VertexConsumer consumer, int overlay, int light, float @Nullable [] tintColor) {
        float r = 1.0F, g = 1.0F, b = 1.0F, a = 1.0F;
        if (tintColor != null) {
            r = Math.max(0, Math.min(1, tintColor[0]));
            g = Math.max(0, Math.min(1, tintColor[1]));
            b = Math.max(0, Math.min(1, tintColor[2]));
            a = Math.max(0, Math.min(1, tintColor[3]));
        }
        for (int nIdx = 0; nIdx < bakedBone.vertexSize(); nIdx++) {
            VertexConsumerPort.vertex(consumer,
                    bakedBone.positionResult()[nIdx * 3],
                    bakedBone.positionResult()[nIdx * 3 + 1],
                    bakedBone.positionResult()[nIdx * 3 + 2],
                    r, g, b, a,
                    bakedBone.u()[nIdx], bakedBone.v()[nIdx], overlay, light,
                    bakedBone.normalResult()[nIdx * 3],
                    bakedBone.normalResult()[nIdx * 3 + 1],
                    bakedBone.normalResult()[nIdx * 3 + 2]
            );
        }
    }

    @Override
    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
    }
}
