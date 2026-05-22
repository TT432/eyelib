package io.github.tt432.eyelib.client.render.visitor;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 模型渲染访问者列表。
 *
 * @author TT432
 */
@NullMarked
public record ModelRenderVisitorList(
        List<ModelVisitor> visitors
) {
}