package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * client entity 的 attachable 渲染开关策略（基岩版 {@code description} 的四个字段）。
 * <p>
 * 为什么需要单独一层：装备渲染有两条路径——真实 {@code minecraft:attachable} 定义，与
 * 代理 attachable（原版盔甲/手持物/头部物品回退，见 {@code ProxyAttachments}）。两条路径若各自
 * 读取开关，就会对同一份数据给出不同结果（例如「有 attachable 定义时盔甲被 hide_armor 关掉，
 * 没定义时却照画」）。本类把判定收敛成一处，两条路径都必须经过它。
 * <p>
 * 字段语义（官方文档原文，bedrock.dev / Microsoft Learn「Client Entity Documentation」）：
 * <ul>
 *   <li>{@code enable_attachables}：「determines if the entity can equip attachables… This allows the
 *       entity to render armor and weapons」——盔甲与武器/手持物的总开关；</li>
 *   <li>{@code hide_armor}：「determines if the armor attached to an entity should be hidden…
 *       This overrides the rendering settings specified by 'enable_attachables'」——只压盔甲；</li>
 *   <li>{@code held_item_ignores_lighting}：手持物是否全亮渲染；</li>
 *   <li>{@code held_item_scale}：Molang，缩放手持物，缺省 1.0，必须解析为 &gt; 0。</li>
 * </ul>
 * holder 没有 client entity 定义（原版实体/未托管类型）时一律允许——与 {@code enable_attachables}
 * 缺省语义无冲突：该字段只存在于萌化实体的定义里。
 *
 * @author TT432
 */
public final class AttachablePolicy {

    private AttachablePolicy() {
    }

    /** holder 的 client entity 定义；未托管/无定义返回 {@code null}（只读，不创建组件）。 */
    public static @Nullable BrClientEntity clientEntityOf(@Nullable Entity holder) {
        if (holder == null) {
            return null;
        }
        RenderData<?> rd = RenderData.peekComponent(holder);
        if (rd == null) {
            return null;
        }
        return rd.getClientEntityComponent().getClientEntity();
    }

    /** {@code enable_attachables}：能否渲染 attachable（盔甲与武器）。无 client entity 视为允许。 */
    public static boolean allowsAttachables(@Nullable BrClientEntity clientEntity) {
        return clientEntity == null || clientEntity.enable_attachables();
    }

    /**
     * 盔甲是否可见：{@code enable_attachables && !hide_armor}。
     * {@code hide_armor} 是 {@code enable_attachables} 的覆盖开关，但仍以总开关为前提。
     */
    public static boolean allowsArmor(@Nullable BrClientEntity clientEntity) {
        return allowsAttachables(clientEntity) && (clientEntity == null || !clientEntity.hide_armor());
    }

    /**
     * 统一准入判定：盔甲走 {@link #allowsArmor}，其它（手持物 / 头饰 / 非盔甲头部物品）走
     * {@link #allowsAttachables}。
     *
     * @param armorStack 该物品在该槽是否属于人形盔甲（版本差异由 {@code isArmorItem} port 吸收）
     */
    public static boolean allows(@Nullable BrClientEntity clientEntity, boolean armorStack) {
        return armorStack ? allowsArmor(clientEntity) : allowsAttachables(clientEntity);
    }

    /**
     * 该槽位物品是否属于盔甲（版本 adapter 判定，天然吸收 BODY 等版本差异）。
     * {@code slot} 为 {@code null}（槽位未知，例如按手持物查询）时按非盔甲处理。
     */
    public static boolean isArmorStack(@Nullable LivingEntity holder, @Nullable EquipmentSlot slot,
                                       ItemStack stack) {
        if (holder == null || slot == null || stack.isEmpty()) {
            return false;
        }
        return RenderPorts.get().renderSystemPort().isArmorItem(stack, slot);
    }

    /** {@code held_item_ignores_lighting}：手持物全亮，否则用实体光照。 */
    public static int heldItemLight(@Nullable BrClientEntity clientEntity, int packedLight) {
        if (clientEntity != null && clientEntity.held_item_ignores_lighting()) {
            return EntityRenderPorts.RenderSystemPort.FULL_BRIGHT;
        }
        return packedLight;
    }

    /**
     * {@code held_item_scale} 求值结果：作用域缺失/求值异常/非正/非有限 → 1.0（不做「缩成 0」的可见退化）。
     */
    public static float heldItemScale(@Nullable BrClientEntity clientEntity, @Nullable MolangScope scope) {
        if (clientEntity == null) {
            return 1F;
        }
        MolangValue value = clientEntity.held_item_scale();
        if (value == null || scope == null) {
            return 1F;
        }
        double raw;
        try {
            raw = value.eval(scope);
        } catch (RuntimeException e) {
            return 1F;
        }
        return sanitizeHeldItemScale(raw);
    }

    /** 纯函数：Bedrock 要求 {@code held_item_scale} 解析为 &gt; 0；其余情况回落 1.0。 */
    public static float sanitizeHeldItemScale(double raw) {
        return raw > 0 && Double.isFinite(raw) ? (float) raw : 1F;
    }
}
