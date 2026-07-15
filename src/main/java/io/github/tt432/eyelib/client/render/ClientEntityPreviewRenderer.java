package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.world.entity.LivingEntity;

/**
 * Reuses the production ClientEntity setup, render-controller, material, texture and animation pipeline
 * for the model preview screen.
 */
public final class ClientEntityPreviewRenderer {
    private final RenderData<LivingEntity> renderData = new RenderData<>();

    public RenderData<LivingEntity> renderData() {
        return renderData;
    }

    public void prepare(BrClientEntity clientEntity, LivingEntity host, float partialTick) {
        renderData.ensureOwner(host);
        if (renderData.getClientEntityComponent().getClientEntity() != clientEntity) {
            renderData.getClientEntityComponent().setClientEntity(clientEntity);
        }

        EntityRenderOrchestrator.setupClientEntity(clientEntity, renderData).forEach(Runnable::run);
        MolangScope scope = renderData.requireScope();
        EntityRenderOrchestrator.setupExtraMolang(host, scope, partialTick);

        AnimationEffects effects = new AnimationEffects();
        ModelRuntimeData pose = renderData.getAnimationComponent().getSerializableInfo() == null
                ? ModelRuntimeData.EMPTY
                : BrAnimator.tickAnimation(
                        renderData.getAnimationComponent(), scope, effects,
                        (ClientTickPort.getTick() + partialTick) / 20F,
                        () -> clientEntity.scripts().ifPresent(scripts -> scripts.pre_animation().eval(scope)));
        renderData.getAnimationComponent().tickedInfos = pose;
        renderData.getAnimationComponent().effects = effects;
    }

    //? if <26.1 {
    public boolean render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                          LivingEntity host, float partialTick) {
        return SimpleRenderAction.builder(bufferSource, RenderSink.of(bufferSource), poseStack, renderData, partialTick)
                .entity(host)
                .animation(renderData.getAnimationComponent())
                .build()
                .render();
    }
    //?}
}
