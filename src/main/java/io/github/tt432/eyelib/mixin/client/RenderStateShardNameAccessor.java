//? if <26.1 {
package io.github.tt432.eyelib.mixin.client;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 RenderType 的短名字。
 *
 * <p>不能靠 {@code RenderType.toString()}：1.20.1 的 {@code CompositeRenderType} 把它重写成
 * 带完整状态的超长串，会导致同名 RenderType（仅纹理不同）被拆成多行统计。
 */
@Mixin(RenderStateShard.class)
public interface RenderStateShardNameAccessor {
    @Accessor("name")
    String eyelib$name();
}
//?}
