package io.github.tt432.eyelib.bridge.client.gui.manager.hotkey;

import io.github.tt432.eyelib.bridge.client.gui.manager.EyelibManagerScreen;
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(value = Dist.CLIENT)
//?}
public final class ManagerScreenOpenEvents {
    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(TickEvent.ClientTickEvent event) {
    //?} else {
    public static void onEvent(ClientTickEvent.Pre event) {
    //?}
        if (ManagerScreenKeybinds.OPEN_SCREEN.isDown() && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(EyelibManagerScreen.create());
        }
    }
}
