package io.github.tt432.eyelib.wintersky;

import java.util.random.RandomGenerator;

/**
 * mulberry32（与 oracle 生成脚本中的 JS 实现逐位一致）。
 */
final class Mulberry32 implements RandomGenerator {
    private int a;

    Mulberry32(int seed) {
        this.a = seed;
    }

    private int draw() {
        a = a + 0x6D2B79F5;
        int t = (a ^ (a >>> 15)) * (1 | a);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return t ^ (t >>> 14);
    }

    @Override
    public double nextDouble() {
        return (draw() & 0xFFFFFFFFL) / 4294967296.0;
    }

    @Override
    public long nextLong() {
        long hi = draw() & 0xFFFFFFFFL;
        long lo = draw() & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }
}
