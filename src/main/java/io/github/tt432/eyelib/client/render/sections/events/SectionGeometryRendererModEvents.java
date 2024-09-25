package io.github.tt432.eyelib.client.render.sections.events;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.sections.AdditionalSectionGeometryBlockEntityRendererDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * @author Argon4W
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = Eyelib.MOD_ID, value = Dist.CLIENT)
public class SectionGeometryRendererModEvents {
    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        AdditionalSectionGeometryBlockEntityRendererDispatcher.CACHE.clear();
    }
}
