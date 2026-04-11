package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author TT432
 */
@UtilityClass
public class ModelRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelRenderer.class);
    private static final AtomicInteger DEBUG_REMAINING = new AtomicInteger(Integer.getInteger("eyelib.debug.uv.limit", 256));

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static void render(RenderParams renderParams, Model model, ModelRuntimeData infos, ModelRenderVisitorList visitors) {
        if (DEBUG_REMAINING.getAndDecrement() > 0) {
            LOGGER.info("[UV-DEBUG][ModelRenderer.render] visitorCount={} visitors={} boneCount={} consumerPresent={}",
                    visitors.visitors().size(),
                    visitors.visitors().stream().map(v -> v.getClass().getSimpleName()).toList(),
                    model.toplevelBones().size(),
                    renderParams.consumer() != null);
        }
        for (ModelVisitor visitor : visitors.visitors()) {
            visitor.visitModel(renderParams, new ModelVisitContext(), cast(infos), model);
        }
    }
}
