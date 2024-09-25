package io.github.tt432.eyelib.client.render.sections.events;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.sections.AdditionalSectionGeometryBlockEntityRendererDispatcher;
import net.minecraft.client.renderer.Sheets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * @author Argon4W
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = Eyelib.MOD_ID, value = Dist.CLIENT)
public class SectionGeometryRendererGameEvents {
    @SubscribeEvent
    public static void onAddSectionGeometry(AddSectionGeometryEvent event) {
        event.addRenderer(new AdditionalSectionGeometryBlockEntityRendererDispatcher(event.getSectionOrigin().immutable()));
    }

    @SubscribeEvent
    public static void onRenderSectionRenderType(RenderLevelStageEvent event)
    {
        if (ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        event.getLevelRenderer().renderSectionLayer(Sheets.translucentItemSheet(), position.x, position.y, position.z, event.getModelViewMatrix().translate(position.negate()), event.getProjectionMatrix());
        event.getLevelRenderer().renderBuffers.bufferSource().endBatch(Sheets.translucentItemSheet());
    }
}
