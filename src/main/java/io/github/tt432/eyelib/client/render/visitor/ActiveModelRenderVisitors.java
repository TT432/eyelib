package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.client.compat.ar.ARCompat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class ActiveModelRenderVisitors {
    public static final ModelVisitor RENDER_VISITOR;

    static {
        if (ARCompat.AR_INSTALLED) {
            RENDER_VISITOR = new ARBakedVisitor();
        } else {
            RENDER_VISITOR = new HighSpeedRenderModelVisitor();
        }
    }
}