package io.github.tt432.eyelib.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GlobalBoneIdHandler 并发契约（Opt19）：static synchronized 保护下，
 * 并发同名 get 返回同 id、异名返回异 id、get(int) 反查一致，无 map 损坏。
 *
 * @author TT432
 */
class GlobalBoneIdHandlerConcurrencyTest {

    @Test
    void concurrentGetSameNameYieldsSameId() throws Exception {
        String bone = "opt19_conc_bone";
        int threads = 8;
        int iterations = 5_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean mismatch = new AtomicBoolean(false);
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
                    int expected = -1;
                    for (int i = 0; i < iterations; i++) {
                        int id = GlobalBoneIdHandler.get(bone);
                        if (expected == -1) {
                            expected = id;
                        } else if (id != expected) {
                            mismatch.set(true);
                            return;
                        }
                        // 反查一致
                        if (!bone.equals(GlobalBoneIdHandler.get(id))) {
                            mismatch.set(true);
                            return;
                        }
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
        assertTrue(!mismatch.get(), "并发 get 出现 id 不一致或反查失败");
    }

    @Test
    void concurrentDistinctNamesYieldDistinctIds() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        int[] ids = new int[threads];
        try {
            Future<?>[] futures = new Future<?>[threads];
            for (int t = 0; t < threads; t++) {
                int idx = t;
                futures[t] = pool.submit(() -> {
                    try {
                        start.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    ids[idx] = GlobalBoneIdHandler.get("opt19_distinct_" + idx);
                });
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        for (int i = 0; i < threads; i++) {
            assertEquals("opt19_distinct_" + i, GlobalBoneIdHandler.get(ids[i]));
            for (int j = i + 1; j < threads; j++) {
                assertTrue(ids[i] != ids[j], "异名必须分配异 id");
            }
        }
    }
}
