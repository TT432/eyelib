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
import lombok.Getter;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
        installInvalidationListener();
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

    private static final AtomicBoolean INVALIDATION_INSTALLED = new AtomicBoolean(false);

    /**
     * 幂等安装 dfsModels 缓存失效监听（与 {@link ModelBakeInvalidationHooks#install()} 同模式）。
     * 不允许用 static initializer 做业务 wiring（ADR-0018 Q-2）；
     * 在 {@link #start()} 中调用，保证首次使用 dfsModels 前监听已就位。
     */
    private static void installInvalidationListener() {
        if (!INVALIDATION_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        ManagerEntryChangedEventPublisher.<ManagerEventPort>addListener(e -> {
            if (e.getManagerName().equals(ModelManager.class.getSimpleName()))
                dfsModels.remove(e.getEntryName());
        });
    }

    public DFSModel dfsModel(Model model) {
        return dfsModels.computeIfAbsent(model.name(), m -> DFSModel.create(model));
    }

    public RenderHelper params(RenderParams params) {
        this.params = params;
        return this;
    }

    public RenderHelper render(RenderParams params, Model model, ModelRuntimeData infos) {
        this.params = params;
        if (params.texture() != null) {
            var meshTexture = params.meshTexture() != null ? params.meshTexture() : params.texture();
            context.put("BackedModel", ModelBakePort.twoSideGetBakedModel(model, params.isSolid(),
                    ResourceLocationBridge.toMc(params.texture()), ResourceLocationBridge.toMc(meshTexture)));
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

