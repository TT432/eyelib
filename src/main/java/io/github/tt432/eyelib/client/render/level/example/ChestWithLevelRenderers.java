package io.github.tt432.eyelib.client.render.level.example;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.client.model.UnbakedModelPart;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.client.model.BakedModels;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

/**
 * @author TT432
 */
@UtilityClass
public class ChestWithLevelRenderers {
    private final BakedModel[][] bakedModel = new BakedModel[3][3];
    private boolean baked;

    private final ResourceLocation builtin = ResourceLocations.mod("builtin");

    private final Material CHEST_MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.withDefaultNamespace("entity/chest/normal"));
    private final Material CHEST_LOCATION_LEFT = new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.withDefaultNamespace("entity/chest/normal_left"));
    private final Material CHEST_LOCATION_RIGHT = new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.withDefaultNamespace("entity/chest/normal_right"));

    private BakedModel[] bakeChestModel(ModelPart modelpart, String prefix, Material material) {
        ModelResourceLocation chestLidLoc = new ModelResourceLocation(builtin, prefix + "_lid");
        ModelResourceLocation chestBottomLoc = new ModelResourceLocation(builtin, prefix + "_bottom");
        ModelResourceLocation chestLockLoc = new ModelResourceLocation(builtin, prefix + "_lock");

        return new BakedModel[]{
                BakedModels.bake(chestLidLoc, new UnbakedModelPart(modelpart.getChild("lid"), material)),
                BakedModels.bake(chestBottomLoc, new UnbakedModelPart(modelpart.getChild("bottom"), material)),
                BakedModels.bake(chestLockLoc, new UnbakedModelPart(modelpart.getChild("lock"), material)),
        };
    }

    public void render(BlockPos pos, BlockPos origin, AddSectionGeometryEvent.SectionRenderingContext context) {
        if (!baked) {
            baked = true;

            EntityModelSet models = Minecraft.getInstance().getEntityModels();

            bakedModel[0] = bakeChestModel(models.bakeLayer(ModelLayers.CHEST), "chest", CHEST_MATERIAL);
            bakedModel[1] = bakeChestModel(models.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT), "double_chest_left", CHEST_LOCATION_LEFT);
            bakedModel[2] = bakeChestModel(models.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT), "double_chest_right", CHEST_LOCATION_RIGHT);
        }

        VertexConsumer buffer = context.getOrCreateChunkBuffer(RenderType.solid());
        BlockAndTintGetter region = context.getRegion();
        int light = LightTexture.pack(region.getBrightness(LightLayer.BLOCK, pos), region.getBrightness(LightLayer.SKY, pos));
        PoseStack poseStack = context.getPoseStack();
        poseStack.pushPose();

        int offsetX = pos.getX() - origin.getX();
        int offsetY = pos.getY() - origin.getY();
        int offsetZ = pos.getZ() - origin.getZ();
        BlockState blockState = region.getBlockState(pos);
        float yRot = blockState.getValue(ChestBlock.FACING).toYRot();

        float blockX = offsetX + 0.5F;
        float blockY = offsetY + 0.5F;
        float blockZ = offsetZ + 0.5F;
        poseStack.translate(blockX, blockY, blockZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.translate(-blockX, -blockY, -blockZ);

        poseStack.translate(offsetX, offsetY, offsetZ);

        switch (blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE) {
            case SINGLE -> {
                for (BakedModel bakedModel : bakedModel[0]) {
                    if (bakedModel == null) continue;
                    BakedModels.render(poseStack.last(), buffer, bakedModel, RenderType.solid(), blockState, light);
                }
            }
            case LEFT -> {
                for (BakedModel bakedModel : bakedModel[1]) {
                    if (bakedModel == null) continue;
                    BakedModels.render(poseStack.last(), buffer, bakedModel, RenderType.solid(), blockState, light);
                }
            }
            case RIGHT -> {
                for (BakedModel bakedModel : bakedModel[2]) {
                    if (bakedModel == null) continue;
                    BakedModels.render(poseStack.last(), buffer, bakedModel, RenderType.solid(), blockState, light);
                }
            }
        }

        poseStack.popPose();
    }
}
