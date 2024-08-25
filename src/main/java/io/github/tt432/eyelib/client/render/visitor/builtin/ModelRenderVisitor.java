package io.github.tt432.eyelib.client.render.visitor.builtin;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

/**
 * @author TT432
 * @see io.github.tt432.eyelib.client.render.ModelRenderer
 */
public class ModelRenderVisitor {
    public void visitModel(RenderParams renderParams) {

    }

    public <G extends Model.Bone, R extends ModelRuntimeData<G, ?, R>> void visitBone(
            RenderParams renderParams, G group, R data, ModelTransformer<G, R> transformer
    ) {

    }

    public void visitCube(RenderParams renderParams, Model.Cube cube) {

    }

    public void visitFace(RenderParams renderParams, Model.Cube cube, List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {

    }

    public void visitVertex(RenderParams renderParams, Model.Cube cube, Vector3fc vertex, Vector2fc uv, Vector3fc normal) {

    }

    public <G extends Model.Bone, R extends ModelRuntimeData<G, ?, R>> void visitLocator(
            RenderParams renderParams, Model.Bone bone, String name, LocatorEntry locator, G group, R data, ModelTransformer<G, R> transformer
    ) {

    }
}
