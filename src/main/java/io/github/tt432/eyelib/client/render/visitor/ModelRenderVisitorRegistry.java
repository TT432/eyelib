package io.github.tt432.eyelib.client.render.visitor;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelRenderVisitorRegistry {
    public static final ResourceLocation VISITOR_REGISTRY_KEY =
            new ResourceLocation(Eyelib.MOD_ID, "model_render_visitor");

    public static Supplier<IForgeRegistry<ModelRenderVisitor>> VISITOR_REGISTRY;

    @SubscribeEvent
    public static void onEvent(NewRegistryEvent event) {
        VISITOR_REGISTRY = event.create(RegistryBuilder.of(VISITOR_REGISTRY_KEY));
    }
}
