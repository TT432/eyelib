/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.example;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.example.client.renderer.entity.*;
import io.github.tt432.eyelib.example.item.GeckoArmorItem;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import io.github.tt432.eyelib.example.client.renderer.armor.GeckoArmorRenderer;
import io.github.tt432.eyelib.example.client.renderer.tile.FertilizerTileRenderer;
import io.github.tt432.eyelib.example.client.renderer.tile.HabitatTileRenderer;
import io.github.tt432.eyelib.example.registry.BlockRegistry;
import io.github.tt432.eyelib.example.registry.EntityRegistry;
import io.github.tt432.eyelib.example.registry.TileRegistry;
import io.github.tt432.eyelib.common.bedrock.renderer.GeoArmorRenderer;

@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientListener {

	@SubscribeEvent
	public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
		if (GeckoLibMod.shouldRegisterExamples()) {
			event.registerEntityRenderer(EntityRegistry.GEO_EXAMPLE_ENTITY.get(), ExampleGeoRenderer::new);
			event.registerEntityRenderer(EntityRegistry.BIKE_ENTITY.get(), BikeGeoRenderer::new);
			event.registerEntityRenderer(EntityRegistry.CAR_ENTITY.get(), CarGeoRenderer::new);
			event.registerEntityRenderer(EntityRegistry.GEOLAYERENTITY.get(), LERenderer::new);
			event.registerEntityRenderer(EntityRegistry.EXTENDED_RENDERER_EXAMPLE.get(),
					ExampleExtendedRendererEntityRenderer::new);
			event.registerEntityRenderer(EntityRegistry.TEXTURE_PER_BONE_EXAMPLE.get(),
					TexturePerBoneTestEntityRenderer::new);

			event.registerBlockEntityRenderer(TileRegistry.HABITAT_TILE.get(), HabitatTileRenderer::new);
			event.registerBlockEntityRenderer(TileRegistry.FERTILIZER.get(), FertilizerTileRenderer::new);

			event.registerEntityRenderer(EntityType.CREEPER, ReplacedCreeperRenderer::new);
		}
	}

	@SubscribeEvent
	public static void registerRenderers(final EntityRenderersEvent.AddLayers event) {
		if (GeckoLibMod.shouldRegisterExamples()) {
			GeoArmorRenderer.registerArmorRenderer(GeckoArmorItem.class, () -> new GeckoArmorRenderer());
		}
	}

	@SubscribeEvent
	public static void registerRenderers(final FMLClientSetupEvent event) {
		if (GeckoLibMod.shouldRegisterExamples()) {
			ItemBlockRenderTypes.setRenderLayer(BlockRegistry.HABITAT_BLOCK.get(), RenderType.cutout());
		}
	}
}
