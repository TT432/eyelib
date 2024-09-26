package io.github.tt432.eyelib.client.render.sections.example;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.client.model.UnbakedModelPart;
import io.github.tt432.eyelib.client.render.sections.ISectionGeometryRenderContext;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.client.BakedModels;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

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

    public void render(AddSectionGeometryEvent.SectionRenderingContext context, PoseStack poseStack, BlockPos pos, BlockPos regionOrigin, ISectionGeometryRenderContext renderContext) {
        if (!baked) {
            baked = true;

            EntityModelSet models = Minecraft.getInstance().getEntityModels();

            bakedModel[0] = bakeChestModel(models.bakeLayer(ModelLayers.CHEST), "chest", CHEST_MATERIAL);
            bakedModel[1] = bakeChestModel(models.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT), "double_chest_left", CHEST_LOCATION_LEFT);
            bakedModel[2] = bakeChestModel(models.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT), "double_chest_right", CHEST_LOCATION_RIGHT);
        }

        BlockState blockState = context.getRegion().getBlockState(pos);
        float yRot = blockState.getValue(ChestBlock.FACING).toYRot();

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.translate(-0.5, -0.5, -0.5);

        int index = switch (blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE) {
            case SINGLE -> 0;
            case LEFT -> 1;
            case RIGHT -> 2;
        };

        for (BakedModel model : bakedModel[index]) {
            if (model != null) {
                renderContext.renderCachedModel(model, poseStack, RenderType.solid(), OverlayTexture.NO_OVERLAY, ModelData.EMPTY);
            }
        }

        poseStack.popPose();
    }
}
