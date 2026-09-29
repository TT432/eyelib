package io.github.tt432.eyelib.wintersky.rng;

import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * wintersky 移植版共享随机源：替代 JS 全局 {@code Math.random()}。
 *
 * <p>这是移植版对原库唯一的有意偏离（见 docs/decisions/0034-wintersky-as-is-port.md）：
 * JS 中 {@code Math.random()} 不可注入，Java 版允许测试注入确定性
 * {@link RandomGenerator}（如 mulberry32 移植）以对齐 Node golden oracle。
 * 默认行为与 JS 相同：非加密、非确定的均匀 double。
 */
public final class WinterskyRandom {

    private static RandomGenerator current = new Random();

    private WinterskyRandom() {
    }

    /** 对应 JS {@code Math.random()}：返回 [0, 1) 均匀 double。 */
    public static double nextDouble() {
        return current.nextDouble();
    }

    public static RandomGenerator current() {
        return current;
    }

    /** 注入随机源（测试用；须在测试结束后 {@link #reset()}）。 */
    public static void set(RandomGenerator generator) {
        current = Objects.requireNonNull(generator, "generator");
    }

    /** 恢复默认随机源。 */
    public static void reset() {
        current = new Random();
    }
}
