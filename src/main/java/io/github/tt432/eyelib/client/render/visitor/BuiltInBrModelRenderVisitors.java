package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.Eyelib;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BuiltInBrModelRenderVisitors {
    public static final DeferredRegister<ModelVisitor> VISITORS =
            DeferredRegister.create(ModelRenderVisitorRegistry.VISITOR_REGISTRY, Eyelib.MOD_ID);

    public static final DeferredHolder<ModelVisitor, RenderModelVisitor> BLANK =
            VISITORS.register("blank", RenderModelVisitor::new);

    public static final DeferredHolder<ModelVisitor, CollectLocatorModelVisitor> COLLECT_LOCATOR =
            VISITORS.register("collect_locator", CollectLocatorModelVisitor::new);

    public static final DeferredHolder<ModelVisitor, HighSpeedRenderModelVisitor> HIGH_SPEED_RENDER =
            VISITORS.register("high_speed", HighSpeedRenderModelVisitor::new);
}
