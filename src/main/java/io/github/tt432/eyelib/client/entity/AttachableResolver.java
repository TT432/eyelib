package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.bridge.client.entity.ItemKeyResolver;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据实体手持物品查找匹配的 attachable 定义。
 * itemKey → 候选列表的索引按 {@link AttachableManager} 的变更代际惰性重建，
 * 热路径为单次 Map 查找；无任何 attachable 注册时直接短路（不触碰 RenderData）。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableResolver {

    /** 索引项：condition 为 null 表示 identifier 直接匹配（无需求值）。 */
    private record Entry(BrClientEntity attachable, @Nullable MolangValue condition) {
    }

    private static volatile long indexGeneration = -1;
    private static volatile Map<String, List<Entry>> index = Map.of();

    /**
     * 按手持物解析 attachable（不区分槽位，按「非盔甲」语义做准入判定）。
     * 已知槽位时请用 {@link #resolve(LivingEntity, ItemStack, EquipmentSlot)}——盔甲槽要额外受
     * {@code hide_armor} 约束。
     */
    @Nullable
    public static BrClientEntity resolve(LivingEntity holder, ItemStack stack) {
        return resolve(holder, stack, null);
    }

    /**
     * 解析 attachable，并按 {@link AttachablePolicy} 做准入判定。
     * <p>
     * 真实 attachable 与代理 attachable（原版盔甲/手持物回退）共用同一判定，避免两条路径对同一份
     * client entity 数据给出不同结果：盔甲槽物品受 {@code enable_attachables && !hide_armor}，
     * 其余物品只受 {@code enable_attachables}。
     *
     * @param slot 槽位；{@code null} 表示未知（按非盔甲处理）
     */
    @Nullable
    public static BrClientEntity resolve(LivingEntity holder, ItemStack stack, @Nullable EquipmentSlot slot) {
        if (stack.isEmpty() || AttachableManager.INSTANCE.all().isEmpty()) {
            return null;
        }

        String itemKey = ItemKeyResolver.getItemKeyString(stack);
        if (itemKey == null) {
            return null;
        }

        RenderData<?> renderData = RenderData.getComponent(holder);
        renderData.ensureOwner(holder);

        BrClientEntity holderCe = renderData.getClientEntityComponent().getClientEntity();
        if (!AttachablePolicy.allows(holderCe, AttachablePolicy.isArmorStack(holder, slot, stack))) {
            return null;
        }

        return resolveByItemId(itemKey, renderData.requireScope());
    }

    /**
     * 判断 holder 的 client entity 是否允许 attachable（不含 {@code hide_armor} 的盔甲细分；
     * 装备渲染一律走 {@link #resolve(LivingEntity, ItemStack, EquipmentSlot)}）。
     * holder 无 client entity 定义（vanilla 实体）时默认允许。
     */
    static boolean isAttachableEnabled(@Nullable BrClientEntity holderCe) {
        return AttachablePolicy.allowsAttachables(holderCe);
    }

    @Nullable
    public static BrClientEntity resolveByItemId(String itemId) {
        List<Entry> candidates = index().get(itemId);
        if (candidates == null) {
            return null;
        }
        for (Entry entry : candidates) {
            // 无 scope 可求值时，声明了 item 条件的 attachable 直接视为匹配（与旧行为一致）
            if (entry.condition() != null || entry.attachable().identifier().equals(itemId)) {
                return entry.attachable();
            }
        }
        return null;
    }

    /**
     * 按 holder 作用域评估 item molang 条件，仅 truthy 才匹配。
     * 当 attachable 未声明 item 字段时，回退到 identifier 直接匹配（BE 规范：
     * identifier 本身是物品 ID 时，无需 item 字段即可直接绑定）。
     */
    @Nullable
    public static BrClientEntity resolveByItemId(String itemId, MolangScope scope) {
        List<Entry> candidates = index().get(itemId);
        if (candidates == null) {
            return null;
        }
        for (Entry entry : candidates) {
            MolangValue condition = entry.condition();
            if (condition != null) {
                if (condition.evalAsBool(scope)) {
                    return entry.attachable();
                }
            } else if (entry.attachable().identifier().equals(itemId)) {
                return entry.attachable();
            }
        }
        return null;
    }

    private static Map<String, List<Entry>> index() {
        long generation = AttachableManager.INSTANCE.generation();
        Map<String, List<Entry>> current = index;
        if (indexGeneration == generation) {
            return current;
        }
        synchronized (AttachableResolver.class) {
            if (indexGeneration == generation) {
                return index;
            }
            Map<String, List<Entry>> rebuilt = new HashMap<>();
            for (BrClientEntity attachable : AttachableManager.INSTANCE.all().values()) {
                attachable.item().forEach((itemKey, condition) ->
                        rebuilt.computeIfAbsent(itemKey, k -> new ArrayList<>())
                               .add(new Entry(attachable, new MolangValue(condition))));
                // identifier 回退项；item 表已声明同键时条件优先，不重复入索引
                if (!attachable.item().containsKey(attachable.identifier())) {
                    rebuilt.computeIfAbsent(attachable.identifier(), k -> new ArrayList<>())
                           .add(new Entry(attachable, null));
                }
            }
            index = rebuilt;
            indexGeneration = generation;
            return rebuilt;
        }
    }
}
