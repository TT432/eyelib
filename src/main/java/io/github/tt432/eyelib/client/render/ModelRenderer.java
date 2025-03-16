package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import lombok.experimental.UtilityClass;

/**
 * @author TT432
 */
@UtilityClass
public class ModelRenderer {
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <R extends ModelRuntimeData<?, ?, R>> void render(RenderParams renderParams, Model model, R infos,
                                                                    ModelRenderVisitorList visitors) {
        for (ModelVisitor visitor : visitors.visitors()) {
            visitor.visitModel(renderParams, new ModelVisitContext(), cast(infos), model);
        }
    }
}
