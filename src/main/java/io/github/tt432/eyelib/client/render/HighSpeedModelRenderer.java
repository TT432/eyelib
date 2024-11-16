package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import lombok.experimental.UtilityClass;

/**
 * @author TT432
 */
@UtilityClass
public class HighSpeedModelRenderer {
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <R extends ModelRuntimeData<?, ?, R>> void render(RenderParams renderParams, Model model, R infos, BakedModel bakedModel) {
        ModelVisitor.Context context = new ModelVisitor.Context();
        context.put("BackedModel", bakedModel);
        BuiltInBrModelRenderVisitors.HIGH_SPEED_RENDER.get().visitModel(renderParams, context, cast(infos), model);
    }
}
