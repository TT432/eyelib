package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.EntityRenderSystem;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import lombok.With;
//? if <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
//? if <1.20.6 {
import net.minecraftforge.client.event.RenderLivingEvent;
//?} else {
import net.neoforged.neoforge.client.event.RenderLivingEvent;
//?}
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
        @Nullable Entity entity,
        @Nullable ModelRuntimeData tickedInfos,
        @Nullable AnimationEffects effects,
        Builder.ExtraRender<T> extraRender
) {
    //? if <26.1 {
    public static <T extends LivingEntity> Builder<T> builder(RenderLivingEvent<T, ?> event) {
        LivingEntity entity = Objects.requireNonNull(event.getEntity());

        return SimpleRenderAction.<T>builder(event.getMultiBufferSource(), event.getPoseStack(), RenderData.getComponent(entity), event.getPartialTick())
                                 .entity(entity)
                                 .overlay(LivingEntityRenderer.getOverlayCoords(entity, ((LivingEntityRendererAccessor) (event.getRenderer())).callGetWhiteOverlayProgress(entity, event.getPartialTick())))
                                 .light(event.getPackedLight());
    }
    //?}

    public static <T> Builder<T> builder(MultiBufferSource multiBufferSource, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
        return new Builder<>(multiBufferSource, poseStack, renderData, partialTick);
    }

    public RenderParams renderParams(ModelComponent modelComponent) {
        RenderParams.Builder builder = RenderParams.builder(poseStack, multiBufferSource, modelComponent);
        float[] colorMask = modelComponent.usesColorMask() ? entityTintColor() : null;
        if (colorMask != null) {
            builder = builder.colorMaskTexture(multiBufferSource, modelComponent, colorMask);
        }
        float[] rcColor = modelComponent.getRcColor();
        if (rcColor != null) {
            builder = builder.tintColor(rcColor);
        }
        return builder
                .entity(entity)
                .overlay(overlay)
                //? if <26.1
                .light(modelComponent.isIgnoreLighting() ? LightTexture.FULL_BRIGHT : packedLight)
                //? if >=26.1
                .light(modelComponent.isIgnoreLighting() ? 0xF000F0 : packedLight)
                .partVisibility(modelComponent.getPartVisibility())
                .build();
    }

    /**
     * 从实体提取 Bedrock color mask 使用的染色颜色。
     */
    private float @Nullable [] entityTintColor() {
        if (entity instanceof Sheep sheep) {
            var dyeColor = sheep.getColor();
            if (dyeColor != null) {
                //? if <1.20.6 {
                float[] diffuse = dyeColor.getTextureDiffuseColors();
                return new float[]{diffuse[0], diffuse[1], diffuse[2], 1.0F};
                //?} else {
                int diffuse = dyeColor.getTextureDiffuseColor();
                return new float[]{
                        net.minecraft.util.FastColor.ARGB32.red(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.green(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.blue(diffuse) / 255.0F,
                        1.0F
                };
                //?}
            }
        }
        return null;
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
        //? if <26.1
        private int light = LightTexture.FULL_BRIGHT;
        //? if >=26.1
        private int light = 0xF000F0;
        private int overlay = OverlayTexture.NO_OVERLAY;
        @Nullable Entity entity;
        @Nullable ModelRuntimeData tickedInfos = ModelRuntimeData.EMPTY;
        @Nullable AnimationEffects effects = new AnimationEffects();
        ExtraRender<T> extraRender = (helper, action) -> {
        };

        public Builder(MultiBufferSource multiBufferSource, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
            this.multiBufferSource = multiBufferSource;
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
