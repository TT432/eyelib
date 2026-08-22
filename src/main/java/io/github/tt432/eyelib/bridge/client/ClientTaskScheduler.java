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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID)
//?} else {
@EventBusSubscriber(modid = Eyelib.MOD_ID)
//?}
/** @author TT432 */
public class ClientTaskScheduler {
    /** 跨线程提交暂存：schedule 可从任意线程调用，tick 起始合并进执行列表。 */
    private static final Queue<Runnable> PENDING = new ConcurrentLinkedQueue<>();
    private static final List<Runnable> TASKS = new ArrayList<>();

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // ClientTickEvent 每 tick 触发 START/END 两次，只在 END 执行
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
    //?} else {
    public static void onClientTick(ClientTickEvent.Pre event) {
    //?}
        if (!PENDING.isEmpty()) {
            for (Runnable task; (task = PENDING.poll()) != null; ) {
                TASKS.add(task);
            }
        }
        if (TASKS.isEmpty()) {
            return;
        }
        // 任务执行中可继续 schedule（进 PENDING，下一 tick 执行），迭代自身无并发风险
        for (Runnable task : TASKS) {
            task.run();
        }
        TASKS.clear();
    }

    /**
     * 安排任务在下一个 client tick 执行一次。可从任意线程调用。
     */
    public static void scheduleNextTick(Runnable task) {
        PENDING.add(task);
    }
}
