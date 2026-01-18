package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.RenderData;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static io.github.tt432.eyelib.client.EntityRenderSystem.renderItemInHand;

/**
 * @author TT432
 */
public class EyelibLivingEntityRenderer<T extends LivingEntity>
        extends LivingEntityRenderer<T, EyelibLivingEntityRenderer.EmptyEntityModel<T>> {
    public EyelibLivingEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new EmptyEntityModel<>(), shadowRadius);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return new ResourceLocation("eyelib", "empty");
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTicks));
        RenderData<Object> cap = RenderData.getComponent(entity);
        SimpleRenderAction.builder(buffer, poseStack, cap, partialTicks)
                .entity(entity)
                .animation(cap.getAnimationComponent())
                .light(packedLight)
                .overlay(overlay)
                .extraRender((helper, action) -> {
                    renderItemInHand(helper, buffer, entity, packedLight);
                    // todo 修改成使用 layer
                })
                .build()
                .render();
    }

    public static class EmptyEntityModel<T extends Entity> extends EntityModel<T> {

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        }
    }
}
