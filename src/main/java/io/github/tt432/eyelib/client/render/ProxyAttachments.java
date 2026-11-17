package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.entity.AttachablePolicy;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 原版装备渲染的代理 attachable 注册表与编排（槽位归属的唯一裁决点）。
 * <p>
 * 渲染顺序复刻 vanilla {@code LivingEntityRenderer}：头部物品层 → 盔甲层（CHEST/LEGS/FEET/HEAD，
 * 与 {@code HumanoidArmorLayer} 内部顺序一致）→ 手持层（副手先、主手后，与 {@code ItemInHandLayer} 一致）。
 * <p>
 * 某个槽若存在真正的 {@code minecraft:attachable} 定义（{@link AttachableResolver} 命中 item 键），
 * 该槽整条跳过——挂载定义优先于原版回退，两条路径不会再同时画一份（历史缺陷："盔甲被画两次"）。
 * <p>
 * 关于「声明」：代理的槽位归属与挂载目标仍由本类声明（{@link ProxyAttachable}），但**执行**统一走
 * {@link AttachmentStage} 的挂载循环——外部自定义挂载（饰品/乘客/携带宠物）与内置代理共用同一份
 * 去重、异常隔离与「锚点缺失即不绘制」语义（G3）。内置代理的锚点在各自 renderer 内解析，
 * 因此使用 {@link AttachmentAnchor#selfAnchored()}。
 * <p>
 * 已知语义差异（不是 bug，是数据缺口，用 {@link #audit(Model)} 逐模型核对）：
 * 模型没有可用挂载骨骼时该装备不绘制（基岩版语义），因此某些萌化模型上不会出现手持物/胸甲。
 *
 * @author TT432
 */
public final class ProxyAttachments {

    private ProxyAttachments() {
    }

    /**
     * 全量代理声明。手持物的候选链依次兜底：{@code rightitem}/{@code leftitem}（基岩版规范挂点）→
     * {@code righthand}/{@code lefthand}（手骨骼）→ {@code right/lefthandlocator}（手部 locator）。
     * 链内只放「手」级别的骨骼：手臂/前臂的 pivot 在肩肘，拿它当挂点会把物品摆到关节上。
     */
    private static final List<ProxyAttachable> ALL = List.of(
            new ProxyAttachable("eyelib:head_item", EquipmentSlot.HEAD, ProxyAttachable.Kind.HEAD_ITEM,
                    List.of(List.of("head"))),
            new ProxyAttachable("eyelib:armor_chest", EquipmentSlot.CHEST, ProxyAttachable.Kind.ARMOR_PIECE,
                    List.of(List.of("body"), List.of("head"))),
            new ProxyAttachable("eyelib:armor_legs", EquipmentSlot.LEGS, ProxyAttachable.Kind.ARMOR_PIECE,
                    List.of(List.of("body"))),
            new ProxyAttachable("eyelib:armor_feet", EquipmentSlot.FEET, ProxyAttachable.Kind.ARMOR_PIECE,
                    List.of(List.of("rightleg"))),
            new ProxyAttachable("eyelib:armor_head", EquipmentSlot.HEAD, ProxyAttachable.Kind.ARMOR_PIECE,
                    List.of(List.of("head"))),
            new ProxyAttachable("eyelib:held_item_off", EquipmentSlot.OFFHAND, ProxyAttachable.Kind.HELD_ITEM,
                    List.of(List.of("leftitem", "lefthand", "lefthandlocator"))),
            new ProxyAttachable("eyelib:held_item_main", EquipmentSlot.MAINHAND, ProxyAttachable.Kind.HELD_ITEM,
                    List.of(List.of("rightitem", "righthand", "righthandlocator")))
    );

    /**
     * 生成代理的挂载声明（每实体每帧一次，先于模型几何提交）。
     * <p>
     * 只返回本帧**应当绘制**的代理：槽位真实 attachable 已接管、物品为空、策略禁止的槽位都不产生声明——
     * 这样下游看到的就是真实裁决结果。声明与实际绘制分段数由 {@link AttachmentStage} 统一执行，
     * 只有真正画出几何的槽位才计入 {@link RenderFrame#handledSlots()}。
     *
     * @param attachableSlots 已由真实 {@code minecraft:attachable} 定义接管的槽位（这些槽不再走代理）
     */
    static List<SlotDeclaration> declarations(SimpleRenderAction<?> action, RenderFrame frame,
                                              LivingEntity holder, Set<EquipmentSlot> attachableSlots) {
        // 与真实 attachable 共用同一份准入策略：代理不是「另一套渲染」，只是「同一套装备渲染的
        // 原版几何来源」。enable_attachables 关 → 全部不画；hide_armor 只压盔甲/头盔（武器照常）。
        BrClientEntity holderCe = AttachablePolicy.clientEntityOf(holder);
        List<SlotDeclaration> declarations = new ArrayList<>(ALL.size());
        for (ProxyAttachable proxy : ALL) {
            if (attachableSlots.contains(proxy.slot()) || !proxy.manages(holder)) {
                continue;
            }
            if (!AttachablePolicy.allows(holderCe, proxy.kind() == ProxyAttachable.Kind.ARMOR_PIECE)) {
                continue;
            }
            AttachmentDraw draw = switch (proxy.kind()) {
                case HELD_ITEM -> (fr, pose, anchorName) ->
                        HandItemRenderer.render(action, fr, holder, proxy) ? 1 : 0;
                case HEAD_ITEM -> (fr, pose, anchorName) ->
                        HeadItemRenderer.render(action, fr, holder, proxy) ? 1 : 0;
                case ARMOR_PIECE -> (fr, pose, anchorName) ->
                        ArmorAttachmentRenderer.render(action, fr, holder, proxy);
            };
            declarations.add(new SlotDeclaration(proxy.slot(),
                    new AttachmentDeclaration(proxy.identifier(), AttachmentAnchor.selfAnchored(), draw)));
        }
        return declarations;
    }

    /** 代理声明与它管理的槽位（槽位用于在真正画出几何后标记 {@link RenderFrame#handledSlots()}）。 */
    record SlotDeclaration(EquipmentSlot slot, AttachmentDeclaration declaration) {
    }

    /**
     * 逐模型核对全部代理的挂载目标（诊断/审计入口）。
     * 每行格式：{@code <identifier> slot=<槽> bind=<候选链> -> <解析到的骨骼|MISSING>}。
     * 审计只看模型自身骨骼，与实体类型/物品无关，因此可以直接对任一模型调用。
     */
    public static List<String> audit(Model model) {
        List<String> lines = new ArrayList<>(ALL.size());
        for (ProxyAttachable proxy : ALL) {
            List<String> targets = new ArrayList<>(proxy.bindTargets().size());
            boolean missing = false;
            for (List<String> chain : proxy.bindTargets()) {
                int bone = ProxyAttachable.resolveChain(model, chain);
                if (bone < 0) {
                    missing = true;
                    targets.add(String.join("|", chain) + "->MISSING");
                } else {
                    targets.add(String.join("|", chain) + "->" + GlobalBoneIdHandler.get(bone));
                }
            }
            lines.add(proxy.identifier() + " slot=" + proxy.slot() + " bind=" + String.join(", ", targets)
                              + (missing ? " (MISSING)" : ""));
        }
        return lines;
    }

    /**
     * 挂载物用的模型：同一实体的各渲染组件共享骨骼位姿，取骨骼最多的组件（基础模型，
     * 覆盖层/发光层的骨骼是其子集）即可解析全部挂载点。
     */
    static @org.jspecify.annotations.Nullable Model attachmentModel(List<ModelComponent> components) {
        Model model = null;
        int best = -1;
        for (ModelComponent component : components) {
            // 只跟已就绪的组件走：模型尚未烘焙时不能只画装备不画身体
            if (!component.readyForRendering()) {
                continue;
            }
            Model candidate = component.getModel();
            if (candidate == null) {
                continue;
            }
            int boneCount = candidate.allBones().size();
            if (boneCount > best) {
                best = boneCount;
                model = candidate;
            }
        }
        return model;
    }

    /** 双手/盔甲槽全空时不做任何挂载物工作（绝大多数实体走这条短路）。 */
    static boolean hasEquipment(LivingEntity holder) {
        return !holder.getMainHandItem().isEmpty()
                || !holder.getOffhandItem().isEmpty()
                || !holder.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !holder.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !holder.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !holder.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }
}
