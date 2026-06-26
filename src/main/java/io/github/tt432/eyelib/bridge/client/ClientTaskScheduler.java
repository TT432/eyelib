package io.github.tt432.eyelib.bridge.client;

import io.github.tt432.eyelib.bridge.Eyelib;
//? if <1.20.6 {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}
import java.util.ArrayList;
import java.util.List;

//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID)
//?} else {
@EventBusSubscriber(modid = Eyelib.MOD_ID)
//?}
/** @author TT432 */
public class ClientTaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onClientTick(TickEvent.ClientTickEvent event) {
    //?} else {
    public static void onClientTick(ClientTickEvent.Pre event) {
    //?}
        for (ScheduledTask task : TASKS) {
            task.tick();
        }
        TASKS.removeIf(task -> task.ran);
    }

    public static void scheduleNextTick(Runnable task) {
        TASKS.add(new ScheduledTask(task, 1));
    }

    public static class ScheduledTask {
        private final Runnable task;
        private int delay;
        private boolean ran;

        public ScheduledTask(Runnable task, int delay) {
            this.task = task;
            this.delay = delay;
        }

        public void tick() {
            if (delay > 0) {
                delay -= 1;
                return;
            }
            if (ran) {
                return;
            }
            task.run();
            ran = true;
        }
    }
}
