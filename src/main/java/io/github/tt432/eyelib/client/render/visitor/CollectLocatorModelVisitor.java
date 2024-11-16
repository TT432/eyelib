package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class CollectLocatorModelVisitor extends ModelVisitor {
    @Override
    public <G extends Model.Bone, R extends ModelRuntimeData<G, ?, R>> void visitLocator(RenderParams renderParams, Context context, Model.Bone bone, LocatorEntry locator, R data, ModelTransformer<G, R> transformer) {
        context.<Map<String, Matrix4f>>orCreate("locators", new HashMap<>()).put(locator.name(), new Matrix4f(renderParams.poseStack().poseStack.getLast().pose()));
    }
}