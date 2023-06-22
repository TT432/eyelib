/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.example;

import io.github.tt432.eyelib.api.bedrock.animation.ModelFetcherManager;
import io.github.tt432.eyelib.common.bedrock.renderer.GeoArmorRenderer;
import io.github.tt432.eyelib.example.registry.BlockRegistry;
import io.github.tt432.eyelib.example.registry.EntityRegistry;
import io.github.tt432.eyelib.example.registry.ItemRegistry;
import io.github.tt432.eyelib.example.registry.TileRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@EventBusSubscriber
public class ExampleMod {
    private static final boolean IS_DEVELOPMENT_ENVIRONMENT = !FMLEnvironment.production;

    public static CreativeModeTab mainTab;

    public ExampleMod() {
        if (shouldRegisterExamples()) {
            IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
            EntityRegistry.ENTITIES.register(bus);
            ItemRegistry.ITEMS.register(bus);
            TileRegistry.TILES.register(bus);
            BlockRegistry.BLOCKS.register(bus);
        }
    }

    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveLevelEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        if (event.getEntity().getUUID() == null) {
            return;
        }

        if (event.getLevel().isClientSide)
            GeoArmorRenderer.LIVING_ENTITY_RENDERERS.values().forEach(instances -> {
                if (instances.containsKey(event.getEntity().getUUID())) {
                    ModelFetcherManager.ModelFetcher<?> beGone = instances.get(event.getEntity().getUUID());
                    ModelFetcherManager.removeModelFetcher(beGone);
                    instances.remove(event.getEntity().getUUID());
                }
            });
    }

    /**
     * Returns whether examples are to be registered. Examples are registered when:
     * <ul>
     *     <li>The mod is running in a development environment; <em>and</em></li>
     * </ul>
     *
     * @return whether the examples are to be registered
     */
    static boolean shouldRegisterExamples() {
        return IS_DEVELOPMENT_ENVIRONMENT;
    }
}
