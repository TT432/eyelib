package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.bridge.client.entity.ItemKeyResolver;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 根据实体手持物品查找匹配的 attachable 定义。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableResolver {
    @Nullable
    public static BrClientEntity resolve(LivingEntity holder, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String itemKey = ItemKeyResolver.getItemKeyString(stack);
        if (itemKey == null) {
            return null;
        }

        RenderData<?> renderData = RenderData.getComponent(holder);
        renderData.ensureOwner(holder);

        if (!isAttachableEnabled(renderData.getClientEntityComponent().getClientEntity())) {
            return null;
        }

        return resolveByItemId(itemKey.toString(), renderData.requireScope());
    }

    /**
     * 判断 holder 的 client entity 是否允许 attachable。
     * holder 无 client entity 定义（vanilla 实体）时默认允许。
     */
    static boolean isAttachableEnabled(@Nullable BrClientEntity holderCe) {
        return holderCe == null || holderCe.enable_attachables();
    }

    @Nullable
    public static BrClientEntity resolveByItemId(String itemId) {
        for (BrClientEntity attachable : AttachableManager.INSTANCE.all().values()) {
            if (attachable.item().containsKey(itemId) || attachable.identifier().equals(itemId)) {
                return attachable;
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
        for (BrClientEntity attachable : AttachableManager.INSTANCE.all().values()) {
            String condition = attachable.item().get(itemId);
            if (condition != null) {
                if (new MolangValue(condition).evalAsBool(scope)) {
                    return attachable;
                }
            } else if (attachable.identifier().equals(itemId)) {
                return attachable;
            }
        }
        return null;
    }
}
