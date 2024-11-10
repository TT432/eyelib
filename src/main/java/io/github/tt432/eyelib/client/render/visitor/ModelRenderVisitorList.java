package io.github.tt432.eyelib.client.render.visitor;

import java.util.List;

/**
 * @author TT432
 */
public record ModelRenderVisitorList(
        List<ModelVisitor> visitors
) {
}
