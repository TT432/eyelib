package io.github.tt432.eyelib.client.render.sections.events;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.sections.SectionGeometryBlockEntityRenderDispatcher;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * @author Argon4W
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = Eyelib.MOD_ID, value = Dist.CLIENT)
public class SectionGeometryRendererModEvents {
    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        SectionGeometryBlockEntityRenderDispatcher.RENDERER_MODEL_CACHE.clear();
    }

    @SubscribeEvent
    public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DynamicChunkBuffers());
    }
}
