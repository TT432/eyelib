package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入 HumanoidArmorLayer，为盔甲槽 attachable 提供渲染路径。
 * 按 EquipmentSlot 独立拦截 renderArmorPiece，有 attachable 则替代 vanilla 盔甲渲染。
 *
 * @author TT432
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Shadow
    @SuppressWarnings({"unchecked", "rawtypes"})
    abstract HumanoidModel getContextModel();

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void eyelib$onRenderArmorPiece(PoseStack poseStack, MultiBufferSource buffer,
                                            LivingEntity entity, EquipmentSlot slot, int light,
                                            HumanoidModel model, CallbackInfo ci) {
        ItemStack item = entity.getItemBySlot(slot);
        if (item.isEmpty()) {
            return;
        }

        BrClientEntity attachable = AttachableResolver.resolve(entity, item);
        if (attachable == null) {
            return;
        }

        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(entity, slot,
                AttachableItemRenderSetup.isLocalPlayerFirstPerson(entity));
        if (rd == null) {
            return;
        }

        HumanoidModel contextModel = getContextModel();
        ModelPart part = eyelib$getArmorPart(contextModel, slot);

        poseStack.pushPose();
        part.translateAndRotate(poseStack);

        AttachableItemRenderSetup.renderAttachable(rd, poseStack, buffer, entity, light,
                OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        ci.cancel();
    }

    private static ModelPart eyelib$getArmorPart(HumanoidModel<?> model, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> model.head;
            case CHEST -> model.body;
            case LEGS -> model.rightLeg;
            case FEET -> model.rightLeg;
            default -> model.body;
        };
    }
}
