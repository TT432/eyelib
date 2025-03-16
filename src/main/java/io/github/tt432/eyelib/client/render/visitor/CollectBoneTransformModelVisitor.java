package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import lombok.AllArgsConstructor;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@AllArgsConstructor
public class CollectBoneTransformModelVisitor extends ModelVisitor  {
    private final String boneName;

    @Override
    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
        context.<Map<String, Matrix4f>>orCreate("bones", new HashMap<>()).put(boneName, new Matrix4f(renderParams.poseStack().poseStack.getLast().pose()));
        super.visitPostBone(renderParams, context, group, data, groupLocator, transformer);
    }
}
