//? if <26.1 {
package io.github.tt432.eyelib.mixin.client;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 {@code AbstractSelectionList#getRowTop}（protected）给 PackEntryMixin
 * 计算包设置齿轮的命中矩形。
 *
 * @author TT432
 */
@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {
    @Invoker("getRowTop")
    int eyelib$getRowTop(int index);
}
//?}
