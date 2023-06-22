package io.github.tt432.eyelib.example.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.renderer.ExtendedGeoEntityRenderer;
import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.client.model.entity.TexturePerBoneTestEntityModel;
import io.github.tt432.eyelib.example.entity.TexturePerBoneTestEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class TexturePerBoneTestEntityRenderer extends ExtendedGeoEntityRenderer<TexturePerBoneTestEntity> {

    public TexturePerBoneTestEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TexturePerBoneTestEntityModel<TexturePerBoneTestEntity>(EntityResources.TEXTUREPERBONE_MODEL, EntityResources.TEXTUREPERBONE_TEXTURE, "textureperbonetestentity"));
    }

    @Override
    protected boolean isArmorBone(Bone bone) {
        return false;
    }

    @Override
    public RenderType getRenderType(TexturePerBoneTestEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected ResourceLocation getTextureForBone(String boneName, TexturePerBoneTestEntity animatable) {
        if (boneName.equalsIgnoreCase("outer_glass")) {
            return EntityResources.TEXTUREPERBONE_GLASS_TEXTURE;
        }
        return null;
    }

    @Override
    protected ItemStack getHeldItemForBone(String boneName, TexturePerBoneTestEntity currentEntity) {
        return null;
    }

    @Override
    protected ItemDisplayContext getCameraTransformForItemAtBone(ItemStack boneItem, String boneName) {
        return null;
    }

    @Override
    protected BlockState getHeldBlockForBone(String boneName, TexturePerBoneTestEntity currentEntity) {
        return null;
    }

    @Override
    protected void preRenderItem(PoseStack matrixStack, ItemStack item, String boneName, TexturePerBoneTestEntity currentEntity, Bone bone) {

    }

    @Override
    protected void preRenderBlock(PoseStack matrixStack, BlockState block, String boneName, TexturePerBoneTestEntity currentEntity) {

    }

    @Override
    protected void postRenderItem(PoseStack matrixStack, ItemStack item, String boneName, TexturePerBoneTestEntity currentEntity, Bone bone) {

    }

    @Override
    protected void postRenderBlock(PoseStack matrixStack, BlockState block, String boneName, TexturePerBoneTestEntity currentEntity) {

    }

}
