package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.compat.ar.ARCompat;
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author TT432
 */
@Setter
public class HighSpeedRenderModelVisitor extends ModelVisitor {
    @Override
    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
        super.visitPreModel(params, context, infos, model);

        if (!context.contains("BackedModel")) {
            throw new RuntimeException("can't use HighSpeedRenderModelVisitor without HBackedModel");
        }
    }

    @Override
    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(poseStack, group, cast(data), transformer);

        var bakedBone = context.<BakedModel>get("BackedModel").bones().get(group.name());

        PoseStack.Pose last = poseStack.last();

        AtomicBoolean render = new AtomicBoolean(renderParams.partVisibility().isEmpty());
        renderParams.partVisibility().forEach((k, v) -> {
            if (!Pattern.compile(k.replace("*", ".*")).matcher(group.name()).matches() || v) {
                render.set(true);
            }
        });

        if (render.get()) {
            if (ARCompat.AR_INSTALLED && ARCompat.renderWithAR(bakedBone, renderParams)) {
                return;
            }

            if (renderParams.consumer() instanceof LazyComputeBufferBuilder lazy && lazy.getEyelib$helper() != null) {
                VertexComputeHelper helper = lazy.getEyelib$helper();
                helper.pushTransform(last.pose(), last.normal(), 0xFF_FF_FF_FF, renderParams.overlay(), renderParams.light());
                helper.addIndex(bakedBone.vertexSize());

                BufferBuilders.putAll((BufferBuilder) lazy, bakedBone.vertices());
            } else {
                bakedBone.transformPos(last.pose());
                bakedBone.transformNormal(last.normal());

                visitVertex(bakedBone, renderParams.consumer(), renderParams.overlay(), renderParams.light());
            }
        }
    }

    @Override
    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
    }

    private static void visitVertex(BakedModel.BakedBone bakedBone, VertexConsumer consumer, int overlay, int light) {
        for (int nIdx = 0; nIdx < bakedBone.vertexSize(); nIdx++) {
            consumer.addVertex(
                    bakedBone.positionResult()[nIdx * 3],
                    bakedBone.positionResult()[nIdx * 3 + 1],
                    bakedBone.positionResult()[nIdx * 3 + 2],
                    0xFF_FF_FF_FF, bakedBone.u()[nIdx], bakedBone.v()[nIdx], overlay, light,
                    bakedBone.normalResult()[nIdx * 3],
                    bakedBone.normalResult()[nIdx * 3 + 1],
                    bakedBone.normalResult()[nIdx * 3 + 2]
            );
        }
    }
}
