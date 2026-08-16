package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.mixin.CamelAccessor;
import io.github.tt432.eyelib.mixin.LivingEntityAccessor;
import io.github.tt432.eyelib.mixin.WolfAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
//? if <26.1 {
import net.minecraft.world.entity.animal.Wolf;
//?} else {
import net.minecraft.world.entity.animal.wolf.Wolf;
//?}
import net.minecraft.world.entity.monster.Creeper;

/**
 * 实体状态查询的版本差异封装，供 molang query（如 {@code client/molang/MolangQuery}）使用。
 *
 * <p>收敛 {@code LivingEntity.jumping}（&lt;1.20.6 公开字段 / 1.21.x 私有需 mixin 访问器 / 26.1 公开方法）
 * 与 {@code Creeper} 膨胀度（{@code swell} 字段 / {@code getSwelling} 方法）的跨版本差异。
 *
 * @author TT432
 */
public interface EntityStatePort {

    /**
     * @return 该生物是否正处于跳跃状态
     */
    static boolean isJumping(LivingEntity entity) {
        //? if <1.20.6 {
        return entity.jumping;
        //?} elif <26.1 {
        return ((LivingEntityAccessor) entity).eyelib$isJumping();
        //?} else {
        return entity.isJumping();
        //?}
    }

    /**
     * @return 苦力怕当前膨胀比例（0~1）
     */
    static float creeperSwell(Creeper creeper) {
        //? if <1.20.6 {
        return creeper.swell / 30F;
        //?} else {
        return creeper.getSwelling(1F);
        //?}
    }

    /**
     * @return 骆驼冲刺是否处于冷却（{@code dashCooldown} 三版本均 private，走 accessor）
     */
    static boolean camelHasDashCooldown(Camel camel) {
        return ((CamelAccessor) camel).eyelib$getDashCooldown() > 0;
    }

    /**
     * @return 狼是否正在抖掉身上的水（{@code isShaking} 三版本均 private，走 accessor）
     */
    static boolean wolfShakingWetness(Wolf wolf) {
        return ((WolfAccessor) wolf).eyelib$isShaking();
    }
}
