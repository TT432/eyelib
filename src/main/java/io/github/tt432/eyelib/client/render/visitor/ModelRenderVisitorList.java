package io.github.tt432.eyelib.client.render.visitor;

import java.util.List;
import org.jspecify.annotations.NullMarked;

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