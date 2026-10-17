package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.model.ModelBakeSink;
import io.github.tt432.eyelib.bridge.client.model.ModelBakeTraversal;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

/**
 * {@link ModelBakeTraversal} 的 application 侧实现：用 {@link ModelVisitor} 遍历模型，
 * 把面/顶点回调转发给 ACL 侧的 {@link ModelBakeSink}（方块几何烘焙）。
 *
 * @author TT432
 */
public final class BedrockModelBakeTraversal implements ModelBakeTraversal {
    @Override
    public void traverse(Model model, PoseStack poseStack, ModelBakeSink sink) {
        ModelVisitor forwarding = new ModelVisitor() {
            @Override
            public void visitFace(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                                  List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {
                sink.face(vertexes, uvs, normal);
            }

            @Override
            public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                                    Vector3fc vertex, Vector2fc uv, Vector3fc normal) {
                sink.vertex(vertex, uv, normal);
            }
        };
        new ModelRenderVisitorList(List.of(forwarding))
                .render(RenderParams.noRender(poseStack), model, ModelRuntimeData.EMPTY);
    }
}
