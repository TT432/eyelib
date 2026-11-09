package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Map;

/**
 * 盔甲渲染：把 JE 人形盔甲部件按基岩版语义绑定到持有者的 body-part 骨骼上。
 * <p>
 * 基岩版的盔甲是 {@code minecraft:attachable}：几何由引擎提供、骨骼按名称绑定到持有者同名骨骼
 * （除 {@code item_slot_to_bone_name} 之外还有 body-part 名称绑定，例如 {@code head}、{@code rightArm}）。
 * JE 侧对应的部件名是 vanilla 命名（{@code head}/{@code body}/{@code right_arm}/…），本类负责两件事：
 * <ol>
 *   <li>部件名 → 持有者骨骼名（大小写/下划线差异在此吸收；找不到骨骼就不画——基岩版同样只在骨骼存在时绑定）；</li>
 *   <li>vanilla 模型空间 → 持有者模型空间的换算。基岩版模型空间相对 JE 实体模型空间是绕 Z 轴 180°
 *       （x、y 取反、z 不变），因此盔甲几何在骨骼帧里需要 {@code scale(-1,-1,1)} 才与基岩版盔甲同向。</li>
 * </ol>
 * 几何、纹理与顶点色由 {@link RenderPorts} 的渲染系统 Port 提供（版本差异集中在那里）。
 *
 * @author TT432
 */
final class ArmorAttachmentRenderer {
    /** 与 vanilla {@code LivingEntityRenderer} 的盔甲层顺序一致。 */
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD
    };

    /** 盔甲是 cutout + 双面可见（vanilla {@code armorCutoutNoCull}）。 */
    private static final PortRenderPass PASS = PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true);

    /** vanilla 部件名 → 持有者骨骼候选名（按序取第一个存在的骨骼）。 */
    private static final Map<String, String[]> PART_BONES = Map.of(
            "head", new String[]{"head"},
            "hat", new String[]{"head"},
            "body", new String[]{"body"},
            "right_arm", new String[]{"rightarm", "right_arm"},
            "left_arm", new String[]{"leftarm", "left_arm"},
            "right_leg", new String[]{"rightleg", "right_leg"},
            "left_leg", new String[]{"leftleg", "left_leg"}
    );

    private ArmorAttachmentRenderer() {
    }

    /**
     * 每实体每帧一次（与 {@link HandItemRenderer} 同一调用点，先于手持物），必须在模型几何提交之前。
     *
     * @param modelRoot 模型根位姿（已含实体位姿；模型 Y 翻转由 {@link ModelPoseTransforms} 施加）
     */
    static void render(SimpleRenderAction<?> action, LivingEntity holder, Model model, ModelRuntimeData tickedInfos,
                       Matrix4f modelRoot, int light) {
        var port = RenderPorts.get().renderSystemPort();
        if (!hasAnyArmor(holder)) {
            return;
        }
        MultiBufferSource bufferSource = action.multiBufferSource();
        int overlay = action.overlay();
        for (EquipmentSlot slot : SLOTS) {
            List<EntityRenderPorts.RenderSystemPort.ArmorRenderPiece> pieces = port.armorPieces(holder, slot);
            for (var piece : pieces) {
                RenderBone bone = resolveBone(model, piece.part());
                if (bone == null) {
                    continue;
                }
                var frame = ModelPoseTransforms.resolveBonePose(model, tickedInfos, bone.boneId(), modelRoot);
                if (frame.isEmpty()) {
                    continue;
                }
                var renderType = MaterialPort.toRenderType(PASS, piece.texture());
                if (renderType == null) {
                    continue;
                }

                PoseStack poseStack = port.createPoseStackFromMatrix(frame.get());
                // vanilla 的 body 盔甲部件原点在颈部（与 head 骨骼同高）；而多数萌化模型的
                // body 骨骼 pivot 在腰部（skeleton 1.114 vs head 1.889，villager 1.063 vs 1.8…），
                // 直接按 body pivot 摆放会把胸甲/护腿整体下移约 0.6~0.8 格。
                // 仍在 body 骨骼帧内（继承躯干旋转/动画），只把锚点换成 head pivot。
                var pivot = "body".equals(piece.part()) ? neckPivot(model, bone) : bone.pivot();
                poseStack.translate(pivot.x(), pivot.y(), pivot.z());
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                piece.emit(poseStack.last(), bufferSource.getBuffer(renderType), light, overlay);
            }
        }
    }

    /** 躯干部件的锚点：模型有 head 骨骼时用其 pivot（颈部），否则退回 body pivot。 */
    private static Vector3fc neckPivot(Model model, RenderBone body) {
        RenderBone head = resolveBone(model, "head");
        return head != null ? head.pivot() : body.pivot();
    }

    private record RenderBone(int boneId, Vector3fc pivot) {
    }

    private static @Nullable RenderBone resolveBone(Model model, String part) {
        String[] candidates = PART_BONES.get(part);
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            int id = GlobalBoneIdHandler.get(candidate);
            Model.Bone bone = model.allBones().get(id);
            if (bone != null) {
                return new RenderBone(id, bone.pivot());
            }
        }
        return null;
    }

    /** 无任何盔甲时提前返回，避免每帧为全部实体做骨骼/纹理解析。 */
    private static boolean hasAnyArmor(LivingEntity holder) {
        for (EquipmentSlot slot : SLOTS) {
            if (!holder.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
