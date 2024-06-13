package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;

import java.util.List;

/**
 * @author TT432
 */
public record ModelRenderVisitorList(
        List<ModelRenderVisitor> visitors
) {
}
