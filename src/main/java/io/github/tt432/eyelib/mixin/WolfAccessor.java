package io.github.tt432.eyelib.mixin;

//? if <26.1 {
import net.minecraft.world.entity.animal.Wolf;
//?} else {
import net.minecraft.world.entity.animal.wolf.Wolf;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@code Wolf#isShaking}（1.20.1/1.21.1/26.1 均 private 无 getter），
 * 供 {@code query.is_shaking_wetness} 实现。
 */
@Mixin(Wolf.class)
public interface WolfAccessor {
    @Accessor("isShaking")
    boolean eyelib$isShaking();
}
