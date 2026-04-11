package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.util.client.texture.TexturePathHelper;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@With
public record RenderParams(
        @Nullable Entity renderTarget,
        PoseStack.Pose pose0,
        PoseStack poseStack,
        @Nullable RenderType renderType,
        @Nullable ResourceLocation texture,
        boolean isSolid,
        @Nullable VertexConsumer consumer,
        int light,
        int overlay,
        Int2BooleanOpenHashMap partVisibility
) {
    public static RenderParams noRender() {
        var poseStack = new PoseStack();
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap()
        );
    }

    public static RenderParams noRender(PoseStack poseStack) {
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap()
        );
    }

    public static Builder builder(PoseStack poseStack, @Nullable RenderType renderType, boolean isSolid, @Nullable ResourceLocation texture, @Nullable VertexConsumer consumer) {
        return new Builder(PoseCopies.copy(poseStack.last()), poseStack, renderType, isSolid, texture, consumer);
    }

    public static Builder builder(PoseStack poseStack, MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        var texture = modelComponent.getTexture();
        if (texture == null) {
            return builder(poseStack, null, modelComponent.isSolid(), null, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        RenderType renderType = modelComponent.getRenderType(texture);
        if (renderType == null) {
            return builder(poseStack, null, modelComponent.isSolid(), texture, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

        return builder(poseStack, renderType, modelComponent.isSolid(), texture, buffer)
                .partVisibility(modelComponent.getPartVisibility());
    }

    public RenderParams asEmissive(MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        if (texture == null) {
            return withTexture(MissingTextureAtlasSprite.getLocation());
        }
        ResourceLocation emissiveTextureLocation = texture.withPath(TexturePathHelper::getEmissiveTexturePath);
        AbstractTexture emissiveTexture = Minecraft.getInstance().getTextureManager()
                .getTexture(emissiveTextureLocation, MissingTextureAtlasSprite.getTexture());

        if (emissiveTexture != MissingTextureAtlasSprite.getTexture()) {
            var emissiveRenderType = modelComponent.getRenderType(emissiveTextureLocation);
            if (emissiveRenderType == null) {
                return withTexture(MissingTextureAtlasSprite.getLocation());
            }
            VertexConsumer emissiveBuffer = multiBufferSource.getBuffer(emissiveRenderType);
            return withRenderType(emissiveRenderType)
                    .withConsumer(emissiveBuffer)
                    .withTexture(emissiveTextureLocation)
                    .withLight(LightTexture.FULL_BRIGHT);
        }

        return withTexture(MissingTextureAtlasSprite.getLocation());
    }

    public boolean textureMissing() {
        return texture == null || texture.equals(MissingTextureAtlasSprite.getLocation());
    }

    public static final class Builder {
        // required
        private final PoseStack.Pose pose0;
        private final PoseStack poseStack;
        @Nullable
        private final RenderType renderType;
        @Nullable
        private final ResourceLocation texture;
        private final boolean isSolid;
        @Nullable
        private final VertexConsumer consumer;

        // optional
        @Nullable
        private Entity renderTarget;
        private int light = LightTexture.FULL_BRIGHT;
        private int overlay = OverlayTexture.NO_OVERLAY;
        private Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();

        public Builder(PoseStack.Pose pose0, PoseStack poseStack, @Nullable RenderType renderType, boolean isSolid, @Nullable ResourceLocation texture, @Nullable VertexConsumer consumer) {
            this.pose0 = pose0;
            this.poseStack = poseStack;
            this.texture = texture;
            this.isSolid = isSolid;
            this.consumer = consumer;
            this.renderType = renderType;
        }

        public Builder entity(@Nullable Entity entity) {
            renderTarget = entity;
            return this;
        }

        public Builder light(int light) {
            this.light = light;
            return this;
        }

        public Builder overlay(int overlay) {
            this.overlay = overlay;
            return this;
        }

        public Builder partVisibility(Int2BooleanOpenHashMap partVisibility) {
            this.partVisibility = partVisibility;
            return this;
        }

        public RenderParams build() {
            return new RenderParams(renderTarget, pose0, poseStack, renderType, texture, isSolid, consumer, light, overlay, partVisibility);
        }
    }
}
