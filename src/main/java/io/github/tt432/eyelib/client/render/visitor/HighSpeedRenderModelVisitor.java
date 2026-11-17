package io.github.tt432.eyelib.client.render.visitor;
import io.github.tt432.eyelib.model.ModelVisitContext;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.client.render.VertexConsumerPort;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
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
        // 根位姿必须在 push + 模型 Y 翻转之前取样：静态实例的几何已含 bind 骨骼链与 180°，
        // 实例矩阵就是这一步的 pose（与蒙皮 palette 的根姿态同空间）。
        PoseStack.Pose rootPose = params.poseStack().last();
        super.visitPreModel(params, context, infos, model);

        if (!context.contains("BackedModel")) {
            throw new RuntimeException("can't use HighSpeedRenderModelVisitor without BackedModel");
        }
        // C1 GPU 蒙皮（ADR-0032）+ C2 静态实例（ADR-0048）：
        // 静态判定 = 定义级静态 或 本帧没有任何骨骼 entry；命中且会话支持 → 实例通道，顶点写入整体跳过。
        SkinningSession skinning = params.skinning();
        if (skinning == null) {
            return;
        }
        BakedModel bakedModel = context.get("BackedModel");
        if (bakedModel == null) {
            return;
        }
        if (staticInstancesAllowed()
                && (params.staticPose() || !infos.hasAnyEntry(model.allBones().keySet()))
                && skinning.beginStatic(bakedModel, model, rootPose.pose(), rootPose.normal(),
                        params.partVisibility(), params.tintColor(), params.overlay(), params.light())) {
            context.put("SkinningSession", skinning);
            context.put("StaticSkinning", Boolean.TRUE);
            return;
        }
        // 静态通道拒绝（几何不可摊平/路径不支持）→ 常规蒙皮；begin 拒绝 → 经典 CPU 路径。
        if (skinning.begin(bakedModel, params.tintColor(), params.overlay(), params.light())) {
            context.put("SkinningSession", skinning);
        }
    }

    /** 静态实例通道是否可用；AR 渲染器持有自己的静态 mesh，覆盖为 false。 */
    protected boolean staticInstancesAllowed() {
        return true;
    }

    @Override
    public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(context, poseStack, bone, cast(data));

        if (context.contains("StaticSkinning")) {
            // 位姿仍要推进：复合 visitor 的 locator 收集依赖同一条骨骼链；此处不写 palette/顶点。
            return;
        }

        var bakedModel = context.<BakedModel>get("BackedModel");
        if (bakedModel == null) {
            return;
        }
        var bakedBone = bakedModel.bones().get(bone.id());
        if (bakedBone == null) {
            return;
        }

        SkinningSession skinning = context.get("SkinningSession");
        boolean visible = renderParams.partVisibility().getOrDefault(bone.id(), true);
        if (skinning != null) {
            PoseStack.Pose last = poseStack.last();
            skinning.appendBone(bone.id(), last.pose(), last.normal());
            if (visible) {
                skinning.markVisible(bone.id());
            }
            return;
        }

        if (visible) {
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
