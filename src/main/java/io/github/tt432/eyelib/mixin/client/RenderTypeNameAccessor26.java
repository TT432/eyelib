//? if >=26.1 {
package io.github.tt432.eyelib.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 26.1.2 RenderType 的短名字（{@code toString()} 带完整状态串，不能直接当名字用）。
 */
@Mixin(RenderType.class)
public interface RenderTypeNameAccessor26 {
    @Accessor("name")
    String eyelib$name();
}
//?}
