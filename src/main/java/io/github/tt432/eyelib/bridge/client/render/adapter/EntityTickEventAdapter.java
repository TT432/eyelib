package io.github.tt432.eyelib.bridge.client.render.adapter;

//? if <26.1 {
import net.minecraft.world.entity.animal.Bee;
//?} else {
import net.minecraft.world.entity.animal.bee.Bee;
//?}
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

/**
 * 实体 tick 事件适配器，处理 Bee swing time 更新等 bridge 内部逻辑。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class EntityTickEventAdapter {
    private EntityTickEventAdapter() {
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(LivingEvent.LivingTickEvent event) {
    //?} else {
    public static void onEvent(EntityTickEvent.Pre event) {
    //?}
        var entity = event.getEntity();
        if (entity instanceof Bee bee) {
            //? if <1.20.6 {
            bee.updateSwingTime();
            //?} elif <26.1 {
            ((io.github.tt432.eyelib.mixin.LivingEntityAccessor) bee).eyelib$invokeUpdateSwingTime();
            //?}
        }
    }
}
