package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.compute.LazyComputeBufferBuilder;
import io.github.tt432.eyelib.compute.VertexComputeHelper;
import io.github.tt432.eyelib.util.client.BufferBuilders;
import lombok.Setter;

/**
 * @author TT432
 */
@Setter
public class HighSpeedRenderModelVisitor extends ModelVisitor {
    @Override
    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitModel(RenderParams params, Context context, D infos, Model model) {
        super.visitModel(params, context, infos, model);

        if (!context.contains("BackedModel")) {
            throw new RuntimeException("can't use HighSpeedRenderModelVisitor without HBackedModel");
        }
    }

    @Override
    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitBone(RenderParams renderParams, Context context, Model model, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(poseStack, group, cast(data), transformer);

        visitLocators(renderParams, context, group, poseStack, data, transformer, groupLocator);

        var bakedBone = context.<BakedModel>get("BackedModel").bones().get(group.name());

        PoseStack.Pose last = poseStack.last();

        if (renderParams.consumer() instanceof LazyComputeBufferBuilder lazy && lazy.getEyelib$helper() != null) {
            VertexComputeHelper helper = lazy.getEyelib$helper();
            helper.pushTransform(last.pose(), last.normal(), 0xFF_FF_FF_FF, renderParams.overlay(), renderParams.light());
            helper.addIndex(bakedBone.nxList().length);

            BufferBuilders.putAll((BufferBuilder) lazy, bakedBone.vertices());
        } else {
            bakedBone.transformPos(last.pose());
            bakedBone.transformNormal(last.normal());

            visitVertex(bakedBone, renderParams.consumer(), renderParams.overlay(), renderParams.light());
        }

        for (var child : group.children().values()) {
            visitBone(renderParams, context, model, child, data, groupLocator.getChild(child.name()), transformer);
        }

        poseStack.popPose();
    }

    private static void visitVertex(BakedModel.BakedBone bakedBone, VertexConsumer consumer, int overlay, int light) {
        for (int nIdx = 0; nIdx < bakedBone.nxList().length; nIdx++) {
            consumer.addVertex(bakedBone.xListResult()[nIdx], bakedBone.yListResult()[nIdx], bakedBone.zListResult()[nIdx],
                    0xFF_FF_FF_FF, bakedBone.u()[nIdx], bakedBone.v()[nIdx], overlay, light,
                    bakedBone.nxListResult()[nIdx], bakedBone.nyListResult()[nIdx], bakedBone.nzListResult()[nIdx]);
        }
    }
}
