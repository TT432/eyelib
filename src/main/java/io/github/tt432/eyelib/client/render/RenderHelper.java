package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bake.TwoSideModelBakeInfo;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * @author TT432
 */
@ParametersAreNonnullByDefault
public class RenderHelper {
    @Getter
    private final ModelVisitor.Context context = new ModelVisitor.Context();
    @Nullable
    private RenderParams params;

    private static final RenderHelper INSTANCE = new RenderHelper();

    private void reset() {
        context.clear();
        params = null;
    }

    public static RenderHelper start() {
        INSTANCE.reset();
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public RenderHelper render(RenderParams params, Model model, BoneRenderInfos infos) {
        this.params = params;
        context.put("BackedModel", TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, params.isSolid(), params.texture()));
        BuiltInBrModelRenderVisitors.HIGH_SPEED_RENDER.get().visitModel(params, context, cast(infos), model);

        return INSTANCE;
    }

    public RenderHelper collectLocators(Model model, BoneRenderInfos infos) {
        if (params != null)
            BuiltInBrModelRenderVisitors.COLLECT_LOCATOR.get().visitModel(params, context, cast(infos), model);
        return this;
    }

    public RenderHelper renderOnLocator(RenderParams params, String visitorName, Model model, BoneRenderInfos infos) {
        if (context.contains("locators")) {
            renderOnLocator(visitorName, model, infos, params);
        }

        return INSTANCE;
    }

    public RenderHelper renderOnLocator(String visitorName, Model model, BoneRenderInfos infos) {
        if (params != null && context.contains("locators")) {
            renderOnLocator(visitorName, model, infos, params);
        }

        return INSTANCE;
    }

    private void renderOnLocator(String visitorName, Model model, BoneRenderInfos infos, RenderParams params) {
        Map<String, Matrix4f> locators = context.get("locators");

        locators.forEach((name, matrix) -> {
            if (name.split("_t_")[0].equals(visitorName)) {
                PoseStack poseStack = new PoseStack();
                poseStack.poseStack.addLast(new PoseStack.Pose(matrix, new Matrix3f()));
                render(params.withPoseStack(poseStack).withPose0(poseStack.last().copy()), model, infos);
            }
        });
    }
}
