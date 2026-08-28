package io.github.tt432.eyelib.molang;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MolangValue 常量池并发契约（Opt19：Float2ObjectOpenHashMap → ConcurrentHashMap）：
 * 多线程 getConstant(同值) 返回同一实例。
 *
 * @author TT432
 */
class MolangValueConstantPoolConcurrencyTest {

    @Test
    void concurrentGetConstantYieldsSameInstance() throws Exception {
        int threads = 8;
        int iterations = 2_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<MolangValue> observed = ConcurrentHashMap.newKeySet();
        try {
            Future<?>[] futures = new Future<?>[threads];
            for (int t = 0; t < threads; t++) {
                futures[t] = pool.submit(() -> {
                    try {
                        start.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < iterations; i++) {
                        observed.add(MolangValue.getConstant(123.5f));
                        observed.add(MolangValue.getConstant(-7.0f));
                    }
                });
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(2, observed.size(), "同值常量必须收敛到同一实例");
    }
}
