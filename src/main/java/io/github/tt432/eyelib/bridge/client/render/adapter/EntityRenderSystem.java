package io.github.tt432.eyelib.bridge.client.render.adapter;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.EntityRenderPorts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
//? if <1.20.6 {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//? if <26.1 {
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
//?} else {
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.jspecify.annotations.Nullable;
//? if <1.20.6 {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/**
 * {@link EntityRenderPorts.RenderSystemPort} 的 Forge 平台实现，
 * 屏蔽版本差异与 mixin accessor 细节，供 application 层通过 Port 接口调用。
 *
 * @author TT432
 */
public final class EntityRenderSystem implements EntityRenderPorts.RenderSystemPort {
    public EntityRenderSystem() {
    }

    @Override
    public String getEntityTypeId(Entity entity) {
        //? if <1.20.6 {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        //?} else {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        //?}
        return key == null ? "" : key.toString();
    }

    @Override
    public int getLlamaDecorColorIndex(Llama llama) {
        //? if <1.20.6 {
        if (llama.inventory.getItem(AbstractHorse.INV_SLOT_ARMOR)
        //?} else {
        if (llama.getInventory().getItem(AbstractHorse.EQUIPMENT_SLOT_OFFSET)
        //?}
                           .getItem() instanceof BlockItem bi && bi.getBlock() instanceof WoolCarpetBlock wc) {
            return wc.getColor().getId() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public void pushPoseRaw(PoseStack poseStack, PoseStack.Pose pose) {
        //? if <1.20.6 {
        poseStack.poseStack.addLast(pose);
        //?} elif <26.1 {
        ((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque().addLast(pose);
        //?}
    }

    @Override
    public void renderItemDirect(LivingEntity le, ItemStack item, ItemDisplayContext context,
                                 boolean left, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        //? if <26.1 {
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                  .renderItem(le, item, context, left, poseStack, bufferSource, light);
        //?}
    }

    @Override
    public void flushBuffer(MultiBufferSource source) {
        //? if <26.1 {
        if (source instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
        //?}
    }

    @Override
    public float @Nullable [] getEntityTintColor(@Nullable Entity entity) {
        if (entity instanceof Sheep sheep) {
            DyeColor dyeColor = sheep.getColor();
            if (dyeColor != null) {
                //? if <1.20.6 {
                float[] diffuse = dyeColor.getTextureDiffuseColors();
                return new float[]{diffuse[0], diffuse[1], diffuse[2], 1.0F};
                //?} else {
                int diffuse = dyeColor.getTextureDiffuseColor();
                //? if <26.1 {
                return new float[]{
                        net.minecraft.util.FastColor.ARGB32.red(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.green(diffuse) / 255.0F,
                        net.minecraft.util.FastColor.ARGB32.blue(diffuse) / 255.0F,
                        1.0F
                };
                //?} else {
                return new float[]{
                        net.minecraft.util.ARGB.red(diffuse) / 255.0F,
                        net.minecraft.util.ARGB.green(diffuse) / 255.0F,
                        net.minecraft.util.ARGB.blue(diffuse) / 255.0F,
                        1.0F
                };
                //?}
                //?}
            }
        }
        return null;
    }

    @Override
    public PoseStack createPoseStackFromMatrix(org.joml.Matrix4f matrix) {
        PoseStack poseStack = new PoseStack();
        //? if <1.20.6 {
        poseStack.poseStack.addLast(new PoseStack.Pose(new org.joml.Matrix4f(matrix), new org.joml.Matrix3f(matrix)));
        //?} elif <26.1 {
        ((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque()
                .addLast(io.github.tt432.eyelib.mixin.PoseStackPoseAccessor.eyelib$create(new org.joml.Matrix4f(matrix), new org.joml.Matrix3f(matrix)));
        //?} else {
        poseStack.last().pose().set(matrix);
        poseStack.last().normal().set(new org.joml.Matrix3f(matrix));
        //?}
        return poseStack;
    }
}
