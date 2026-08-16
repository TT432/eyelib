package io.github.tt432.eyelib.mixin;

import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@code Camel#dashCooldown}（1.20.1/1.21.1/26.1 均为 private 且无 getter），
 * 供 {@code query.has_dash_cooldown} 实现。
 */
@Mixin(Camel.class)
public interface CamelAccessor {
    @Accessor("dashCooldown")
    int eyelib$getDashCooldown();
}
