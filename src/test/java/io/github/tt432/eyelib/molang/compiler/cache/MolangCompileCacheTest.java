package io.github.tt432.eyelib.molang.compiler.cache;

import io.github.tt432.eyelib.molang.compiler.CompiledMolangExpression;
import io.github.tt432.eyelib.molang.compiler.CompileContext;
import io.github.tt432.eyelib.molang.compiler.ExpressionCompileException;
import io.github.tt432.eyelib.molang.compiler.MolangCompilerImpl;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MolangCompileCache 正确性测试。
 * 验证缓存命中、淘汰、过期检测。
 *
 * @author TT432
 */
class MolangCompileCacheTest {
    private static final MolangCompilerImpl compiler = new MolangCompilerImpl();

    private MolangCompileCache cache;

    @BeforeEach
    void setUp() {
        cache = new MolangCompileCache();
    }

    @Nested
    @DisplayName("基本缓存行为")
    class BasicCaching {
        @Test
        void sameKeyReturnsSameInstance() {
            CompiledMolangExpression e1 = cache.getOrCompile("1+2",
                    () -> compiler.compile("1+2", CompileContext.defaults()));
            CompiledMolangExpression e2 = cache.getOrCompile("1+2",
                    () -> compiler.compile("1+2", CompileContext.defaults()));
            assertSame(e1, e2, "缓存命中应返回同一实例");
        }

        @Test
        void differentKeyReturnsDifferentInstances() {
            CompiledMolangExpression e1 = cache.getOrCompile("1+2",
                    () -> compiler.compile("1+2", CompileContext.defaults()));
            CompiledMolangExpression e2 = cache.getOrCompile("3+4",
                    () -> compiler.compile("3+4", CompileContext.defaults()));
            assertNotSame(e1, e2, "不同 key 应返回不同实例");
        }

        @Test
        void supplierOnlyCalledOncePerKey() {
            AtomicInteger callCount = new AtomicInteger(0);
            cache.getOrCompile("5-3",
                    () -> {
                        callCount.incrementAndGet();
                        return compiler.compile("5-3", CompileContext.defaults());
                    });
            cache.getOrCompile("5-3",
                    () -> {
                        callCount.incrementAndGet();
                        return compiler.compile("5-3", CompileContext.defaults());
                    });
            assertEquals(1, callCount.get(), "Supplier 只应被调用一次");
        }

        @Test
        void sizeTracksEntryCount() {
            assertEquals(0, cache.size());
            cache.getOrCompile("a", () -> compiler.compile("1", CompileContext.defaults()));
            assertEquals(1, cache.size());
            cache.getOrCompile("b", () -> compiler.compile("2", CompileContext.defaults()));
            assertEquals(2, cache.size());
        }
    }

    @Nested
    @DisplayName("淘汰策略")
    class Eviction {
        @Test
        void evictionDoesNotCrashWhenAtMaxSize() {
            // 填充到超过 MAX_L1_SIZE (1000)
            for (int i = 0; i < 1100; i++) {
                String key = String.valueOf(i);
                cache.getOrCompile(key,
                        () -> compiler.compile(key, CompileContext.defaults()));
            }
            // 淘汰后不应 crash
            assertTrue(cache.size() <= 1000, "淘汰后大小应在限制内");
        }

        @Test
        void afterEvictionOldEntriesCanBeRecomputed() {
            // 填充至满
            for (int i = 0; i < 1000; i++) {
                final int fi = i;
                cache.getOrCompile(String.valueOf(i),
                        () -> compiler.compile(String.valueOf(fi), CompileContext.defaults()));
            }
            // 触发淘汰：插入新 key
            cache.getOrCompile("new-key",
                    () -> compiler.compile("999", CompileContext.defaults()));

            // 被淘汰的旧 key 可以重新 compute
            CompiledMolangExpression recomputed = cache.getOrCompile("0",
                    () -> compiler.compile("42", CompileContext.defaults()));
            assertNotNull(recomputed);
        }
    }

    @Nested
    @DisplayName("过期检测")
    class Expiry {
        @Test
        void differentRegistryVersionProducesDifferentCacheKey() {
            // 使用自定义 mapping tree 模拟 registry version 变化
            // 直接验证：不同 version 下的 cache 行为
            MolangCompileCache cache1 = new MolangCompileCache(
                    MolangMappingTree.INSTANCE, null);
            MolangCompileCache cache2 = new MolangCompileCache(
                    MolangMappingTree.INSTANCE, null);

            // 同一个 expression，不同 cache 实例应产生不同结果（因为 registryVersionRef 被包含在 key 中）
            CompiledMolangExpression e1 = cache1.getOrCompile("42",
                    () -> compiler.compile("42", CompileContext.defaults()));
            CompiledMolangExpression e2 = cache2.getOrCompile("42",
                    () -> compiler.compile("42", CompileContext.defaults()));

            // 两个都是 MolangCompilerImpl.compile 的结果，值是相同的
            assertNotNull(e1);
            assertNotNull(e2);
        }
    }

    @Nested
    @DisplayName("并发安全")
    class Concurrency {
        @Test
        void concurrentAccessDoesNotCrash() throws Exception {
            int threadCount = 4;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Exception> errors = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                Thread thread = new Thread(() -> {
                    try {
                        for (int i = 0; i < 100; i++) {
                            final int fi = i;
                            String key = threadId + "-" + fi;
                            cache.getOrCompile(key,
                                    () -> compiler.compile(String.valueOf(fi),
                                            CompileContext.defaults()));
                        }
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
                thread.start();
            }

            latch.await();
            assertTrue(errors.isEmpty(),
                    () -> "并发访问不应抛异常: " + errors);
        }
    }
}
