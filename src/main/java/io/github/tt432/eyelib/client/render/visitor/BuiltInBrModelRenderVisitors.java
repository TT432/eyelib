package io.github.tt432.eyelib.client.render.visitor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BuiltInBrModelRenderVisitors {

    public static final RenderModelVisitor BLANK = new RenderModelVisitor();

    public static final CollectLocatorModelVisitor COLLECT_LOCATOR = new CollectLocatorModelVisitor();

    public static final HighSpeedRenderModelVisitor HIGH_SPEED_RENDER = new HighSpeedRenderModelVisitor();
}
