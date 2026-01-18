package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.util.client.Textures;
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
        RenderType renderType,
        ResourceLocation texture,
        boolean isSolid,
        VertexConsumer consumer,
        int light,
        int overlay,
        Int2BooleanOpenHashMap partVisibility
) {
    public static Builder builder(PoseStack poseStack, RenderType renderType, boolean isSolid, ResourceLocation texture, VertexConsumer consumer) {
        return new Builder(poseStack.last().copy(), poseStack, renderType, isSolid, texture, consumer);
    }

    public static Builder builder(PoseStack poseStack, MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        var texture = modelComponent.getTexture();
        RenderType renderType = modelComponent.getRenderType(texture);
        VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

        return builder(poseStack, renderType, modelComponent.isSolid(), texture, buffer)
                .partVisibility(modelComponent.getPartVisibility());
    }

    public RenderParams asEmissive(MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        ResourceLocation emissiveTextureLocation = texture.withPath(Textures::getEmissiveTexturePath);
        AbstractTexture emissiveTexture = Minecraft.getInstance().getTextureManager()
                .getTexture(emissiveTextureLocation, MissingTextureAtlasSprite.getTexture());

        if (emissiveTexture != MissingTextureAtlasSprite.getTexture()) {
            var emissiveRenderType = modelComponent.getRenderType(emissiveTextureLocation);
            VertexConsumer emissiveBuffer = multiBufferSource.getBuffer(emissiveRenderType);
            return withRenderType(emissiveRenderType)
                    .withConsumer(emissiveBuffer)
                    .withTexture(emissiveTextureLocation)
                    .withLight(LightTexture.FULL_BRIGHT);
        }

        return withTexture(MissingTextureAtlasSprite.getLocation());
    }

    public boolean textureMissing() {
        return texture.equals(MissingTextureAtlasSprite.getLocation());
    }

    public static final class Builder {
        // required
        private final PoseStack.Pose pose0;
        private final PoseStack poseStack;
        private final RenderType renderType;
        private final ResourceLocation texture;
        private final boolean isSolid;
        private final VertexConsumer consumer;

        // optional
        private Entity renderTarget;
        private int light = LightTexture.FULL_BRIGHT;
        private int overlay = OverlayTexture.NO_OVERLAY;
        private Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();

        public Builder(PoseStack.Pose pose0, PoseStack poseStack, RenderType renderType, boolean isSolid, ResourceLocation texture, VertexConsumer consumer) {
            this.pose0 = pose0;
            this.poseStack = poseStack;
            this.texture = texture;
            this.isSolid = isSolid;
            this.consumer = consumer;
            this.renderType = renderType;
        }

        public Builder entity(Entity entity) {
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
