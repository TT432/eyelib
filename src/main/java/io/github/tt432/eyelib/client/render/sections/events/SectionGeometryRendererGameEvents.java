package io.github.tt432.eyelib.client.render.sections.events;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.render.sections.SectionGeometryBlockEntityRenderDispatcher;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * @author Argon4W
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = Eyelib.MOD_ID, value = Dist.CLIENT)
public class SectionGeometryRendererGameEvents {
    @SubscribeEvent
    public static void onAddSectionGeometry(AddSectionGeometryEvent event) {
        event.addRenderer(new SectionGeometryBlockEntityRenderDispatcher(event.getSectionOrigin().immutable()));
    }

    @SubscribeEvent
    public static void onRenderVanillaChunkBufferItems(RenderLevelStageEvent event) {
        if (ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        Matrix4f modelViewMatrix = new Matrix4f(event.getModelViewMatrix()).translate(new Vector3f(position).negate());

        event.getLevelRenderer().renderSectionLayer(Sheets.translucentItemSheet(), position.x, position.y, position.z, modelViewMatrix, event.getProjectionMatrix());
        event.getLevelRenderer().renderBuffers.bufferSource().endBatch(Sheets.translucentItemSheet());
    }

    @SubscribeEvent
    public static void onRenderVanillaDynamicCutoutRenderType(RenderLevelStageEvent event) {
        if (ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        Matrix4f modelViewMatrix = new Matrix4f(event.getModelViewMatrix()).translate(new Vector3f(position).negate());

        for (RenderType renderType : DynamicChunkBuffers.DYNAMIC_CUTOUT_LAYERS.values()) {
            event.getLevelRenderer().renderSectionLayer(renderType, position.x, position.y, position.z, modelViewMatrix, event.getProjectionMatrix());
            event.getLevelRenderer().renderBuffers.bufferSource().endBatch(renderType);
        }
    }

    @SubscribeEvent
    public static void onRenderSodiumDynamicCutoutRenderType(RenderLevelStageEvent event) {
        if (!ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        Matrix4f modelViewMatrix = new Matrix4f(event.getModelViewMatrix());

        for (RenderType renderType : DynamicChunkBuffers.DYNAMIC_CUTOUT_LAYERS.values()) {
            event.getLevelRenderer().renderSectionLayer(renderType, position.x, position.y, position.z, modelViewMatrix, event.getProjectionMatrix());
            event.getLevelRenderer().renderBuffers.bufferSource().endBatch(renderType);
        }
    }

    @SubscribeEvent
    public static void onRenderVanillaDynamicTranslucentRenderType(RenderLevelStageEvent event) {
        if (ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        Matrix4f modelViewMatrix = new Matrix4f(event.getModelViewMatrix()).translate(new Vector3f(position).negate());

        for (RenderType renderType : DynamicChunkBuffers.DYNAMIC_TRANSLUCENT_LAYERS.values()) {
            event.getLevelRenderer().renderSectionLayer(renderType, position.x, position.y, position.z, modelViewMatrix, event.getProjectionMatrix());
            event.getLevelRenderer().renderBuffers.bufferSource().endBatch(renderType);
        }
    }

    @SubscribeEvent
    public static void onRenderSodiumDynamicTranslucentRenderType(RenderLevelStageEvent event) {
        if (!ModList.get().isLoaded("sodium")) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Vector3f position = event.getCamera().getPosition().toVector3f();
        Matrix4f modelViewMatrix = new Matrix4f(event.getModelViewMatrix());

        for (RenderType renderType : DynamicChunkBuffers.DYNAMIC_TRANSLUCENT_LAYERS.values()) {
            event.getLevelRenderer().renderSectionLayer(renderType, position.x, position.y, position.z, modelViewMatrix, event.getProjectionMatrix());
            event.getLevelRenderer().renderBuffers.bufferSource().endBatch(renderType);
        }
    }
}
