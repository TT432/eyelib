package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.EntityRenderSystem;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import lombok.With;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * @author TT432
 */
@With
public record SimpleRenderAction<T>(
        MultiBufferSource multiBufferSource,
        PoseStack poseStack,
        RenderData<T> renderData,
        float partialTick,
        int packedLight,
        int overlay,
        Entity entity,
        BoneRenderInfos tickedInfos,
        AnimationEffects effects,
        Builder.ExtraRender<T> extraRender
) {
    public static <T extends LivingEntity> Builder<T> builder(RenderLivingEvent<T, ?> event) {
        LivingEntity entity = event.getEntity();

        return SimpleRenderAction.<T>builder(event.getMultiBufferSource(), event.getPoseStack(), RenderData.getComponent(entity), event.getPartialTick())
                .entity(entity)
                .overlay(LivingEntityRenderer.getOverlayCoords(entity, ((LivingEntityRendererAccessor) (event.getRenderer())).callGetWhiteOverlayProgress(entity, event.getPartialTick())))
                .light(event.getPackedLight());
    }

    public static <T> Builder<T> builder(MultiBufferSource multiBufferSource, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
        return new Builder<>(multiBufferSource, poseStack, renderData, partialTick);
    }

    public RenderParams renderParams(ModelComponent modelComponent) {
        return RenderParams.builder(poseStack, multiBufferSource, modelComponent)
                .entity(entity)
                .overlay(overlay)
                .light(packedLight)
                .partVisibility(modelComponent.getPartVisibility())
                .build();
    }

    public boolean render() {
        return EntityRenderSystem.renderEntity(this);
    }

    public boolean animationNotNull() {
        return tickedInfos != null && effects != null;
    }

    public static class Builder<T> {
        // required
        MultiBufferSource multiBufferSource;
        PoseStack poseStack;
        RenderData<T> renderData;
        float partialTick;

        // optional
        private int light = LightTexture.FULL_BRIGHT;
        private int overlay = OverlayTexture.NO_OVERLAY;
        Entity entity;
        BoneRenderInfos tickedInfos = BoneRenderInfos.EMPTY;
        AnimationEffects effects = new AnimationEffects();
        ExtraRender<T> extraRender = (helper, action) -> {
        };

        public Builder(MultiBufferSource multiBufferSource, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
            this.multiBufferSource = multiBufferSource;
            this.poseStack = poseStack;
            this.partialTick = partialTick;
            this.renderData = renderData;
        }

        public Builder<T> entity(Entity entity) {
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

        public Builder<T> animation(BoneRenderInfos tickedInfos, AnimationEffects effects) {
            this.tickedInfos = tickedInfos;
            this.effects = effects;
            return this;
        }

        public Builder<T> animation(AnimationComponent animationComponent) {
            return animation(animationComponent.tickedInfos, animationComponent.effects);
        }

        public interface ExtraRender<T> {
            void render(RenderHelper helper, SimpleRenderAction<T> action);
        }

        public Builder<T> extraRender(ExtraRender<T> consumer) {
            this.extraRender = consumer;
            return this;
        }

        public SimpleRenderAction<T> build() {
            return new SimpleRenderAction<>(multiBufferSource, poseStack, renderData, partialTick, light, overlay, entity, tickedInfos, effects, extraRender);
        }
    }
}
