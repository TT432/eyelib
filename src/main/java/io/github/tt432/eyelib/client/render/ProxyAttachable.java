package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 代理 attachable：把「原版（vanilla）装备渲染」表达成与 {@code minecraft:attachable} 同形的挂载定义。
 * <p>
 * 为什么需要：萌化实体取消 {@code LivingEntityRenderer} 后，原版三层（{@code CustomHeadLayer} /
 * {@code HumanoidArmorLayer} / {@code ItemInHandLayer}）都不再执行，必须用基岩版语义重新表达。
 * 这些「原版渲染」过去散落在 {@link HeadItemRenderer}、{@link ArmorAttachmentRenderer}、
 * {@link HandItemRenderer} 三处硬编码里：槽位归属靠调用方手写、挂载目标（持有者骨骼名）各自隐式约定，
 * 于是「模型缺一个挂载骨骼」表现为渲染静默跳过，既没有日志也没有统一核对点。
 * <p>
 * 本类型把每一类原版渲染收敛成一条**声明**：
 * <ul>
 *   <li>它管理哪个 {@link EquipmentSlot}（{@link #slot()}，HEAD 槽再按「是否盔甲物品」细分）；</li>
 *   <li>它需要哪些持有者骨骼（{@link #bindTargets()}：每个内层是一条候选链，按序取第一个存在的骨骼）；</li>
 *   <li>由哪种几何绘制（{@link Kind}）。</li>
 * </ul>
 * 槽位编排统一在 {@link ProxyAttachments}，挂载目标是否存在可用
 * {@link ProxyAttachments#audit(Model)} 逐模型核对。
 *
 * @param identifier  诊断/审计用的稳定 id（{@code eyelib:held_item_main} 等）
 * @param slot        管理的装备槽
 * @param kind        挂载单元的几何来源
 * @param bindTargets 挂载目标候选链（单一挂载点的槽位只用第一条链）
 * @author TT432
 */
record ProxyAttachable(String identifier, EquipmentSlot slot, Kind kind, List<List<String>> bindTargets) {

    /** 挂载单元的几何来源：决定由哪个渲染器接手。 */
    enum Kind {
        /** 手持物：匹配到 attachable 时用 attachable 模型，否则画原版物品模型。 */
        HELD_ITEM,
        /** 头部物品（非盔甲）：原版 {@code CustomHeadLayer} 的非盔甲分支。 */
        HEAD_ITEM,
        /** 人形盔甲部件：按部件名绑定到持有者 body-part 骨骼。 */
        ARMOR_PIECE
    }

    /**
     * 该槽在本实体上是否由本代理接管。
     * HEAD 槽由「头部物品」与「头盔」两条代理共享，按物品是否盔甲分流（与 vanilla 层顺序一致）。
     */
    boolean manages(LivingEntity holder) {
        ItemStack stack = holder.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return false;
        }
        boolean armor = RenderPorts.get().renderSystemPort().isArmorItem(stack, slot);
        return switch (kind) {
            case HELD_ITEM -> true;
            case HEAD_ITEM -> !armor;
            case ARMOR_PIECE -> armor;
        };
    }

    /**
     * 解析单一挂载点的持有者骨骼 id：沿第一条候选链取第一个存在于 {@code model} 的骨骼。
     * 返回 -1 表示该模型没有可用挂载目标——调用方**不绘制**（基岩版没有「找不到挂载点就退回模型原点」
     * 的可用语义，任何兜底位置都是错位）。
     */
    int resolveBindTarget(Model model) {
        if (bindTargets.isEmpty()) {
            return -1;
        }
        return resolveChain(model, bindTargets.get(0));
    }

    /** 沿一条候选链解析持有者骨骼 id；链内全部缺失时返回 -1。 */
    static int resolveChain(Model model, List<String> candidates) {
        for (String candidate : candidates) {
            int id = GlobalBoneIdHandler.get(candidate);
            if (model.allBones().containsKey(id)) {
                return id;
            }
        }
        return -1;
    }
}
