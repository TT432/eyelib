package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.client.ClientTickHandler;
import lombok.Getter;
import net.minecraft.client.Minecraft;

/**
 * 固定步进计时器。
 *
 * <p>客户端以 20 tick/s 推进（Minecraft 常规速率），
 * 本计时器以固定 {@link #rate} 步进率（30 step/s）计算自启动以来应前进的固定步数，
 * 并通过 {@code canNextStep()} 判断当前步是否可以进一步；若可以则使 {@link #lastFixed} 自增 1。
 *
 * <p>线程安全：通过同步方法保证并发场景下的状态一致性。
 */
@Getter
public class FixedTimer {

    /**
     * 固定步进率（step/s）。
     */
    private final int rate = 30;

    private boolean init = false;

    /**
     * 最近一次获取时的累计固定步数（自启动以来）。
     */
    private int lastFixed = 0;

    /**
     * 计时起点的客户端 tick 值。
     */
    private int startTicks = 0;

    /**
     * 计时起点的客户端 partialTick 值。
     */
    private float startPartialTick = 0f;

    /**
     * 判断当前步是否可以进一步，并在可以时推进一步。
     *
     * <p>根据客户端的全局 tick 数（20 tick/s）与 partialTick（当前 tick 进度，0.0-1.0）
     * 计算自启动以来的累计固定步数，并与 {@link #lastFixed} 比较：
     * 若当前累计步数大于 {@code lastFixed}，则说明可以进入下一步；此时将 {@code lastFixed} 加 1 并返回 true。
     * 否则返回 false，状态不变。
     *
     * @return 是否可以推进到下一步
     */
    public boolean canNextStep() {
        var secondsSinceStart = realSec();

        int currentFixed = (int) Math.floor(secondsSinceStart * rate);

        if (currentFixed > lastFixed) {
            lastFixed += 1;
            return true;
        }

        if (!init) {
            init = true;
            return true;
        }

        return false;
    }

    /**
     * 设置计时起点，并重置累计固定步数。
     */
    public void start() {
        int ticks = ClientTickHandler.getTick();
        var partialTick = Minecraft.getInstance().timer.partialTick;
        this.startTicks = ticks;
        this.startPartialTick = partialTick;
        this.lastFixed = 0;
    }

    public float realSec() {
        int ticks = ClientTickHandler.getTick();
        var partialTick = Minecraft.getInstance().timer.partialTick;
        return ((ticks + partialTick) - (startTicks + startPartialTick)) / 20;
    }

    /**
     * 将步数转换为对应的秒数。
     *
     * <p>转换公式：seconds = steps / rate。
     * 在本类中 {@link #rate} = 30 step/s，因此等价于 {@code steps / 30.0}。
     *
     * @return 对应的秒数
     */
    public float seconds() {
        return lastFixed / (float) rate;
    }
}