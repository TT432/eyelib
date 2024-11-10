package io.github.tt432.eyelib.testmod.client.events;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import io.github.tt432.eyelib.client.render.sections.events.ReloadDynamicChunkBufferEvent;
import io.github.tt432.eyelib.testmod.EyelibTestMod;
import io.github.tt432.eyelib.testmod.client.blocks.TestBenchBlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = Eyelib.MOD_ID, value = Dist.CLIENT)
public class TestBenchClientModEvents {
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EyelibTestMod.TEST_BENCH_BLOCK_ENTITY_TYPE.get(), context -> new TestBenchBlockEntityRenderer());
    }

    @SubscribeEvent
    public static void onReloadDynamicChunkBuffers(ReloadDynamicChunkBufferEvent event) {
        DynamicChunkBuffers.markMultiCutoutChunkBuffer(EntityType.BOGGED, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/bogged.png"), ResourceLocation.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png"));
    }
}
