package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.Eyelib;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID)
public class ClientScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
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

        private int delay = 0;
        private boolean ran = false;

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
