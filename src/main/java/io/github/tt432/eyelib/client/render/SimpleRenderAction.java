package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.ModelVisitContext;
import lombok.With;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
@With
public record SimpleRenderAction<T>(
        MultiBufferSource multiBufferSource,
        RenderSink sink,
        PoseStack poseStack,
        RenderData<T> renderData,
        float partialTick,
        int packedLight,
        int overlay,
        @Nullable Entity entity,
        @Nullable ModelRuntimeData tickedInfos,
        @Nullable AnimationEffects effects,
        Builder.ExtraRender<T> extraRender
) {
    public static <T> Builder<T> builder(MultiBufferSource multiBufferSource, RenderSink sink, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
        return new Builder<>(multiBufferSource, sink, poseStack, renderData, partialTick);
    }

    public boolean render() {
        return EntityRenderOrchestrator.renderEntity(this);
    }

    public boolean animationNotNull() {
        return tickedInfos != null && effects != null;
    }

    public static class Builder<T> {
        MultiBufferSource multiBufferSource;
        RenderSink sink;
        PoseStack poseStack;
        RenderData<T> renderData;
        float partialTick;

        int light = EntityRenderPorts.RenderSystemPort.FULL_BRIGHT;
        private int overlay = OverlayTexture.NO_OVERLAY;
        @Nullable Entity entity;
        @Nullable ModelRuntimeData tickedInfos = ModelRuntimeData.EMPTY;
        @Nullable AnimationEffects effects = new AnimationEffects();
        ExtraRender<T> extraRender = (context, action) -> {
        };

        public Builder(MultiBufferSource multiBufferSource, RenderSink sink, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
            this.multiBufferSource = multiBufferSource;
            this.sink = sink;
            this.poseStack = poseStack;
            this.partialTick = partialTick;
            this.renderData = renderData;
        }

        public Builder<T> entity(@Nullable Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder<T> light(int light) {
            this.light = light;
            return this;
        }

        public Builder<T> overlay(int overlay) {
            this.overlay = overlay;
            return this;
        }

        public Builder<T> animation(@Nullable ModelRuntimeData tickedInfos, @Nullable AnimationEffects effects) {
            this.tickedInfos = tickedInfos != null ? tickedInfos : ModelRuntimeData.EMPTY;
            this.effects = effects != null ? effects : new AnimationEffects();
            return this;
        }

        public Builder<T> animation(AnimationComponent animationComponent) {
            return animation(animationComponent.tickedInfos, animationComponent.effects);
        }

        public interface ExtraRender<T> {
            void render(ModelVisitContext context, SimpleRenderAction<T> action);
        }

        public Builder<T> extraRender(ExtraRender<T> consumer) {
            this.extraRender = consumer;
            return this;
        }

        public SimpleRenderAction<T> build() {
            return new SimpleRenderAction<>(multiBufferSource, sink, poseStack, renderData, partialTick, light, overlay, entity, tickedInfos, effects, extraRender);
        }
    }
}
