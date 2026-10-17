package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;
import lombok.experimental.UtilityClass;

/**
 * 按 visitor 列表逐个派发模型渲染；DFSModel 缓存与失效钩子的装配在 {@link RenderHelper}。
 *
 * @author TT432
 */
@UtilityClass
public class ModelRenderer {
    public static void render(RenderParams renderParams, Model model, ModelRuntimeData infos, ModelRenderVisitorList visitors) {
        for (ModelVisitor visitor : visitors.visitors()) {
            visitor.visitModel(renderParams, new ModelVisitContext(), infos, model);
        }
    }
}
