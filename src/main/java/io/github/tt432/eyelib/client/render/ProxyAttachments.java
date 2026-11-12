package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

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
     * 每实体每帧一次（先于模型几何提交）。
     *
     * @param attachableSlots 已由真实 {@code minecraft:attachable} 定义接管的槽位（这些槽不再走代理）
     */
    static int render(SimpleRenderAction<?> action, LivingEntity holder, Model model, ModelRuntimeData tickedInfos,
                      Matrix4f modelRoot, int light, Set<EquipmentSlot> attachableSlots) {
        int drawn = 0;
        for (ProxyAttachable proxy : ALL) {
            if (attachableSlots.contains(proxy.slot()) || !proxy.manages(holder)) {
                continue;
            }
            // 挂载目标缺失的槽位不画、也不计入「画了挂载物」：没有任何几何被提交
            int drawnByProxy = switch (proxy.kind()) {
                case HELD_ITEM -> HandItemRenderer.render(action, holder, model, tickedInfos, modelRoot, proxy, light)
                        ? 1 : 0;
                case HEAD_ITEM -> HeadItemRenderer.render(action, holder, model, tickedInfos, modelRoot, proxy, light)
                        ? 1 : 0;
                case ARMOR_PIECE -> ArmorAttachmentRenderer.render(action, holder, model, tickedInfos, modelRoot, proxy,
                        light);
            };
            drawn += drawnByProxy;
        }
        return drawn;
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
}
