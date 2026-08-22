package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.compat.ar.ARCompat;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.model.locator.LocatorEntry;

/**
 * @author TT432
 */
public final class ActiveModelRenderVisitors {
    public static final ModelVisitor RENDER_VISITOR = ARCompat.isArInstalled()
            ? new ARBakedVisitor()
            : new HighSpeedRenderModelVisitor();

    /**
     * 单遍 DFS 复合 visitor：渲染回调委托 {@link #RENDER_VISITOR}，locator 回调委托
     * {@link BuiltInBrModelRenderVisitors#COLLECT_LOCATOR}。两者在 DFS 帧序列上的姿态状态一致，
     * 合并后每组件每帧只走一遍骨骼遍历（原 render + collectLocators 双 DFS）。
     */
    public static final ModelVisitor RENDER_WITH_LOCATOR = new ModelVisitor() {
        private final ModelVisitor render = RENDER_VISITOR;
        private final ModelVisitor locatorCollector = BuiltInBrModelRenderVisitors.COLLECT_LOCATOR;

        @Override
        public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            render.visitPreModel(params, context, infos, model);
        }

        @Override
        public void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            render.visitPostModel(params, context, infos, model);
        }

        @Override
        public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            render.visitPreBone(renderParams, context, bone, data);
        }

        @Override
        public void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, ModelRuntimeData data) {
            render.visitPostBone(renderParams, context, group, data);
        }

        @Override
        public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
            render.visitCube(renderParams, context, cube);
        }

        @Override
        public void visitLocator(RenderParams renderParams, ModelVisitContext context, Model.Bone bone,
                                 LocatorEntry locator, ModelRuntimeData data) {
            locatorCollector.visitLocator(renderParams, context, bone, locator, data);
        }
    };

    private ActiveModelRenderVisitors() {
    }
}
