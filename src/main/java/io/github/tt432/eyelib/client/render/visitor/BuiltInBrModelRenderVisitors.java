package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.visitor.builtin.BlankEntityModelRenderVisitor;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BuiltInBrModelRenderVisitors {
    public static final DeferredRegister<ModelRenderVisitor> VISITORS =
            DeferredRegister.create(ModelRenderVisitorRegistry.VISITOR_REGISTRY_KEY, Eyelib.MOD_ID);

    public static final RegistryObject<BlankEntityModelRenderVisitor > BLANK =
            VISITORS.register("blank", BlankEntityModelRenderVisitor::new);
}
