package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;

import java.util.List;

/**
 * 模型渲染访问者列表；{@link #render} 是“按 visitor 派发一遍遍历”的唯一入口
 * （实体渲染走 {@code RenderHelper}，方块几何烘焙走 bridge 的 traversal Port）。
 *
 * @author TT432
 */
public record ModelRenderVisitorList(
        List<ModelVisitor> visitors
) {
    public void render(RenderParams params, Model model, ModelRuntimeData infos) {
        for (ModelVisitor visitor : visitors) {
            visitor.visitModel(params, new ModelVisitContext(), infos, model);
        }
    }
}
