package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.bridge.client.compat.ar.ARCompat;
/**
 * @author TT432
 */
public final class ActiveModelRenderVisitors {
    public static final ModelVisitor RENDER_VISITOR = ARCompat.isArInstalled()
            ? new ARBakedVisitor()
            : new HighSpeedRenderModelVisitor();

    private ActiveModelRenderVisitors() {
    }
}