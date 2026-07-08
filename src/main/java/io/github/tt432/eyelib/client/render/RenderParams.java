package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort;
import io.github.tt432.eyelib.bridge.client.render.texture.TexturePresencePort;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.texture.TexturePaths;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.With;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
@With
public record RenderParams(
        @Nullable Entity renderTarget,
        PoseStack.Pose pose0,
        PoseStack poseStack,
        @Nullable RenderType renderType,
        @Nullable PortResourceLocation texture,
        boolean isSolid,
        @Nullable VertexConsumer consumer,
        int light,
        int overlay,
        Int2BooleanOpenHashMap partVisibility,
        float @Nullable [] tintColor
) {
    public static RenderParams noRender() {
        var poseStack = new PoseStack();
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap(), null
        );
    }

    public static RenderParams noRender(PoseStack poseStack) {
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap(), null
        );
    }

    public static Builder builder(PoseStack poseStack, @Nullable RenderType renderType, boolean isSolid, @Nullable PortResourceLocation texture, @Nullable VertexConsumer consumer) {
        return new Builder(PoseCopies.copy(poseStack.last()), poseStack, renderType, isSolid, texture, consumer);
    }

    public static Builder builder(PoseStack poseStack, MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        var portTexture = modelComponent.getTexture();
        if (portTexture == null) {
            return builder(poseStack, null, modelComponent.isSolid(), null, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        PortRenderPass renderPass = modelComponent.getRenderType(portTexture);
        RenderType renderType = renderPass != null ? MaterialPort.toRenderType(renderPass, portTexture) : null;
        if (renderType == null) {
            return builder(poseStack, null, modelComponent.isSolid(), portTexture, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

        return builder(poseStack, renderType, modelComponent.isSolid(), portTexture, buffer)
                .partVisibility(modelComponent.getPartVisibility());
    }

    public RenderParams asEmissive(MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        // 26.1 尚未迁移 emissive 重渲染（TextureManager / render-state API 差异），保留原 no-op。
        //? if <26.1 {
        if (texture == null) {
            return withTexture(TexturePresencePort.missingLocation());
        }
        PortResourceLocation emissivePortTexture = PortResourceLocation.of(
                texture.namespace(), TexturePaths.emissivePath(texture.path()));
        if (!TexturePresencePort.isLoaded(emissivePortTexture)) {
            return withTexture(TexturePresencePort.missingLocation());
        }
        PortRenderPass emissiveRenderPass = modelComponent.getRenderType(emissivePortTexture);
        if (emissiveRenderPass == null) {
            return withTexture(TexturePresencePort.missingLocation());
        }
        RenderType emissiveRenderType = MaterialPort.toRenderType(emissiveRenderPass, emissivePortTexture);
        VertexConsumer emissiveBuffer = multiBufferSource.getBuffer(emissiveRenderType);
        return withRenderType(emissiveRenderType)
                .withConsumer(emissiveBuffer)
                .withTexture(emissivePortTexture)
                .withLight(EntityRenderPorts.RenderSystemPort.FULL_BRIGHT);
        //?} else {
        return this;
        //?}
    }

    public boolean textureMissing() {
        return texture == null || texture.equals(TexturePresencePort.missingLocation());
    }

    public static final class Builder {
        // required
        private final PoseStack.Pose pose0;
        private final PoseStack poseStack;
        @Nullable
        private RenderType renderType;
        @Nullable
        private PortResourceLocation texture;
        private boolean isSolid;
        @Nullable
        private VertexConsumer consumer;

        // optional
        @Nullable
        private Entity renderTarget;
        private int light = EntityRenderPorts.RenderSystemPort.FULL_BRIGHT;
        private int overlay = OverlayTexture.NO_OVERLAY;
        private Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
        private float @Nullable [] tintColor = null;

        public Builder(PoseStack.Pose pose0, PoseStack poseStack, @Nullable RenderType renderType, boolean isSolid, @Nullable PortResourceLocation texture, @Nullable VertexConsumer consumer) {
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

        public Builder tintColor(float @Nullable [] tintColor) {
            this.tintColor = tintColor;
            return this;
        }

        public Builder colorMaskTexture(MultiBufferSource multiBufferSource, ModelComponent modelComponent, float[] color) {
            if (texture == null) {
                return this;
            }
            PortResourceLocation colorMaskTexture = NativeImagePort.colorMaskTexture(texture, color);
            if (colorMaskTexture == null) {
                return this;
            }
            PortRenderPass colorMaskPass = modelComponent.getRenderType(colorMaskTexture);
            if (colorMaskPass == null) {
                return this;
            }
            RenderType colorMaskRenderType = MaterialPort.toRenderType(colorMaskPass, colorMaskTexture);
            texture = colorMaskTexture;
            renderType = colorMaskRenderType;
            consumer = multiBufferSource.getBuffer(colorMaskRenderType);
            isSolid = modelComponent.isSolid();
            return this;
        }

        public RenderParams build() {
            return new RenderParams(renderTarget, pose0, poseStack, renderType, texture, isSolid, consumer, light, overlay, partVisibility, tintColor);
        }
    }
}
