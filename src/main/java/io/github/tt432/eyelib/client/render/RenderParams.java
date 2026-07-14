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
import io.github.tt432.eyelib.model.lod.LodRuntimeState;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.With;
import net.minecraft.client.renderer.MultiBufferSource;
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
        @Nullable PortRenderPass renderPass,
        @Nullable PortResourceLocation texture,
        boolean isSolid,
        @Nullable VertexConsumer consumer,
        int light,
        int overlay,
        Int2BooleanOpenHashMap partVisibility,
        float @Nullable [] tintColor,
        @Nullable LodRuntimeState lodState
) {
    public static RenderParams noRender() {
        var poseStack = new PoseStack();
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap(), null, null
        );
    }

    public static RenderParams noRender(PoseStack poseStack) {
        return new RenderParams(
                null, poseStack.last(), poseStack, null, null, false,
                null, 0, OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap(), null, null
        );
    }

    public static Builder builder(PoseStack poseStack, @Nullable PortRenderPass renderPass, boolean isSolid, @Nullable PortResourceLocation texture, @Nullable VertexConsumer consumer) {
        return new Builder(PoseCopies.copy(poseStack.last()), poseStack, renderPass, isSolid, texture, consumer);
    }

    public static Builder builder(PoseStack poseStack, MultiBufferSource multiBufferSource, ModelComponent modelComponent) {
        var portTexture = modelComponent.getTexture();
        if (portTexture == null) {
            return builder(poseStack, null, modelComponent.isSolid(), null, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        PortRenderPass renderPass = modelComponent.getRenderType(portTexture);
        var renderType = renderPass != null ? MaterialPort.toRenderType(renderPass, portTexture) : null;
        if (renderType == null) {
            return builder(poseStack, null, modelComponent.isSolid(), portTexture, null)
                    .partVisibility(modelComponent.getPartVisibility());
        }
        VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

        return builder(poseStack, renderPass, modelComponent.isSolid(), portTexture, buffer)
                .partVisibility(modelComponent.getPartVisibility());
    }


    public boolean textureMissing() {
        return texture == null || texture.equals(TexturePresencePort.missingLocation());
    }

    public static final class Builder {
        // required
        private final PoseStack.Pose pose0;
        private final PoseStack poseStack;
        @Nullable
        private PortRenderPass renderPass;
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
        @Nullable
        private LodRuntimeState lodState;

        public Builder(PoseStack.Pose pose0, PoseStack poseStack, @Nullable PortRenderPass renderPass, boolean isSolid, @Nullable PortResourceLocation texture, @Nullable VertexConsumer consumer) {
            this.pose0 = pose0;
            this.poseStack = poseStack;
            this.texture = texture;
            this.isSolid = isSolid;
            this.consumer = consumer;
            this.renderPass = renderPass;
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

        public Builder lodState(@Nullable LodRuntimeState lodState) {
            this.lodState = lodState;
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
            var colorMaskRenderType = MaterialPort.toRenderType(colorMaskPass, colorMaskTexture);
            texture = colorMaskTexture;
            renderPass = colorMaskPass;
            consumer = multiBufferSource.getBuffer(colorMaskRenderType);
            isSolid = modelComponent.isSolid();
            return this;
        }

        public RenderParams build() {
            return new RenderParams(renderTarget, pose0, poseStack, renderPass, texture, isSolid, consumer, light, overlay, partVisibility, tintColor, lodState);
        }
    }
}
