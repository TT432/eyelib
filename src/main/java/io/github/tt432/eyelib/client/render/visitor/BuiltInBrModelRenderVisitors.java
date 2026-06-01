package io.github.tt432.eyelib.client.render.visitor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class BuiltInBrModelRenderVisitors {

    public static final RenderModelVisitor BLANK = new RenderModelVisitor();

    public static final CollectLocatorModelVisitor COLLECT_LOCATOR = new CollectLocatorModelVisitor();

    public static final HighSpeedRenderModelVisitor HIGH_SPEED_RENDER = new HighSpeedRenderModelVisitor();

    public static final ARBakedVisitor AR_BAKED = new ARBakedVisitor();
}