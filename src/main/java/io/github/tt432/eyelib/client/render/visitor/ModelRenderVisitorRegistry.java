package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * @author TT432
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelRenderVisitorRegistry {
    public static final ResourceKey<Registry<ModelRenderVisitor>> VISITOR_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocations.of(Eyelib.MOD_ID, "model_render_visitor"));

    public static final Registry<ModelRenderVisitor> VISITOR_REGISTRY = new RegistryBuilder<>(VISITOR_REGISTRY_KEY)
            .sync(true)
            .create();

    @SubscribeEvent
    public static void onEvent(NewRegistryEvent event) {
        event.register(VISITOR_REGISTRY);
    }
}
