package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.EyelibClient;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bake.TwoSideModelBakeInfo;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.compute.LazyComputeBufferBuilder;
import io.github.tt432.eyelib.compute.VertexComputeHelper;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@ParametersAreNonnullByDefault
public class RenderHelper {
    private static Boolean _canUseHighSpeedRender = null;

    private static final Map<RenderType, Pair<VertexComputeHelper, MultiBufferSource>> helpers = new Object2ObjectOpenHashMap<>();

    private static boolean canUseHighSpeedRender() {
        if (_canUseHighSpeedRender == null) {
            _canUseHighSpeedRender = EyelibClient.supportComputeShader() && !ModList.get().isLoaded("iris");
        }
        return _canUseHighSpeedRender;
    }

    public RenderHelper openHighSpeedRender(RenderType renderType, MultiBufferSource multiBufferSource, VertexConsumer buffer) {
        if (canUseHighSpeedRender() && buffer instanceof LazyComputeBufferBuilder lazy) {
            var helper = helpers.computeIfAbsent(renderType, r -> Pair.of(new VertexComputeHelper(), multiBufferSource));
            lazy.setEyelib$helper(helper.left());
        }

        return this;
    }

    public RenderHelper openHighSpeedRender(RenderParams params, MultiBufferSource multiBufferSource) {
        return openHighSpeedRender(params.renderType(), multiBufferSource, params.consumer());
    }

    @Getter
    private final ModelVisitContext context = new ModelVisitContext();
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

    private static final Map<String, DFSModel> dfsModels = new HashMap<>();

    public DFSModel dfsModel(Model model) {
        return dfsModels.computeIfAbsent(model.name(), m -> DFSModel.create(model));
    }

    public RenderHelper params(RenderParams params) {
        this.params = params;
        return this;
    }

    {
        NeoForge.EVENT_BUS.addListener(ManagerEntryChangedEvent.class, e -> {
            if (e.getManagerName().equals(ModelManager.class.getSimpleName()))
                dfsModels.remove(e.getEntryName());
        });
    }

    public RenderHelper render(RenderParams params, Model model, BoneRenderInfos infos) {
        this.params = params;
        context.put("BackedModel", TwoSideModelBakeInfo.INSTANCE.getBakedModel(model, params.isSolid(), params.texture()));

        dfsModel(model).visit(params, context, BuiltInBrModelRenderVisitors.HIGH_SPEED_RENDER.get(), cast(infos), new DFSModel.StateMachine());

        return INSTANCE;
    }

    public RenderHelper collectLocators(Model model, BoneRenderInfos infos) {
        if (params != null)
            dfsModel(model).visit(params, context, BuiltInBrModelRenderVisitors.COLLECT_LOCATOR.get(), cast(infos), new DFSModel.StateMachine());
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
                render(params.withPoseStack(poseStack), model, infos);
            }
        });
    }
}
