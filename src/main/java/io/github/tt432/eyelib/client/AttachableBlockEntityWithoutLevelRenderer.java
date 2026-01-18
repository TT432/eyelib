package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * @author TT432
 */
public class AttachableBlockEntityWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    public AttachableBlockEntityWithoutLevelRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
//        RenderData<ItemStack> data = AttchableHandler.getRendingItemStackRenderData(stack);
//        var entity = AttchableHandler.getRenderingEntity();
//
//        if (data != null && entity != null) {
//            MolangScope scope = data.getScope();
//            var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
//            scope.set("variable.partial_tick",partialTick);
////            scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration()); TODO:fix
//
//            ClientEntityComponent clientEntityComponent = data.getClientEntityComponent();
//
//            List<ModelComponent> components = setupClientEntity(entity, clientEntityComponent, data);
//
//            AnimationEffects effects = new AnimationEffects();
//
//            BoneRenderInfos tickedInfos;
//            if (data.getAnimationComponent().getSerializableInfo() != null) {
//                tickedInfos = BrAnimator.tickAnimation(data.getAnimationComponent(), scope, effects,
//                        (ClientTickHandler.getTick() + partialTick) / 20, () -> {
//                            clientEntityComponent.getClientEntity().scripts().ifPresent(scripts -> {
//                                scripts.pre_animation().eval(scope);
//                            });
//                        });
//            } else {
//                tickedInfos = BoneRenderInfos.EMPTY;
//            }
//
//            setupExtraMolang(entity, data);
//
//            var rendered = renderComponents(buffer,poseStack, packedLight,
//                    packedOverlay,partialTick, entity, data, components, tickedInfos, effects, c->{});
//            if (!rendered) {
//                super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
//            }
//
//            scope.remove("variable.partial_tick");
//        } else {
//            super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
//        }
    }
}
