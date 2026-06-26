package io.github.tt432.eyelib.bridge.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}
/**
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(value = Dist.CLIENT)
//?}
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientTickHandler {
    static int tick;

    public static int getTick() {
        return tick;
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (!Minecraft.getInstance().isPaused() && event.phase == TickEvent.Phase.START)
            tick++;
    //?} else {
    public static void onEvent(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().isPaused())
            tick++;
    //?}
    }
}
