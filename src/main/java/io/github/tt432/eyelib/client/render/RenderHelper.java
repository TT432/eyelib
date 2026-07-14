package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.client.model.ModelBakeInvalidationHooks;
import io.github.tt432.eyelib.bridge.event.ManagerEventPort;
import io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.client.render.visitor.ActiveModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.lod.LodRuntimeState;
import lombok.Getter;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class RenderHelper {
    @Getter
    private final ModelVisitContext context = new ModelVisitContext();
    @Nullable
    private RenderParams params;

    public static RenderHelper start() {
        ModelBakeInvalidationHooks.install();
        return new RenderHelper();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    private static final Map<String, DFSModel> dfsModels = new HashMap<>();

    public static int getDfsModelsSize() {
        return dfsModels.size();
    }

    public DFSModel dfsModel(Model model) {
        return dfsModels.computeIfAbsent(model.name(), m -> DFSModel.create(model));
    }

    public RenderHelper params(RenderParams params) {
        this.params = params;
        return this;
    }

    {
        ManagerEntryChangedEventPublisher.<ManagerEventPort>addListener(e -> {
            if (e.getManagerName().equals(ModelManager.class.getSimpleName()))
                dfsModels.remove(e.getEntryName());
        });
    }

    public RenderHelper render(RenderParams params, Model model, ModelRuntimeData infos) {
        this.params = params;
        if (params.lodState() != null) {
            context.put(LodRuntimeState.MODEL_VISIT_CONTEXT_KEY, params.lodState());
        }
        if (params.texture() != null) {
            context.put("BackedModel", ModelBakePort.twoSideGetBakedModel(model, params.isSolid(), ResourceLocationBridge.toMc(params.texture())));
        }

        dfsModel(model).visit(params, context, ActiveModelRenderVisitors.RENDER_VISITOR, infos, new DFSModel.StateMachine());

        return this;
    }

    public RenderHelper collectLocators(Model model, ModelRuntimeData infos) {
        if (params != null)
            dfsModel(model).visit(params, context, BuiltInBrModelRenderVisitors.COLLECT_LOCATOR, cast(infos), new DFSModel.StateMachine());
        return this;
    }

    public RenderHelper renderOnLocator(RenderParams params, String visitorName, Model model, ModelRuntimeData infos) {
        if (context.contains("locators")) {
            renderOnLocator(visitorName, model, infos, params);
        }

        return this;
    }

    public RenderHelper renderOnLocator(String visitorName, Model model, ModelRuntimeData infos) {
        if (params != null && context.contains("locators")) {
            renderOnLocator(visitorName, model, infos, params);
        }

        return this;
    }

    private void renderOnLocator(String visitorName, Model model, ModelRuntimeData infos, RenderParams params) {
        Map<String, Matrix4f> locators = context.get("locators");
        if (locators == null) {
            return;
        }

        locators.forEach((name, matrix) -> {
            if (name.split("_t_")[0].equals(visitorName)) {
                PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(matrix);
                render(params.withPoseStack(poseStack), model, infos);
            }
        });
    }
}

