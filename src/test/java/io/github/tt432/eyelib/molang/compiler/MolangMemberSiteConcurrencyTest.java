package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MemberSite 并发解析契约（Opt19）：表达式实例按字符串全局共享（compileCache），
 * 并行 stage 下多 worker 并发 resolve 同一站点。volatile Binding 快照保证：
 * 读侧永远拿到 (tree, epoch, minimal, full) 一致四元组——并发求值结果与串行基线一致，
 * 纪元切换中不撕裂、不抛异常。
 *
 * @author TT432
 */
class MolangMemberSiteConcurrencyTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void concurrentResolveMatchesSerialBaseline() throws Exception {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ConcMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_conc + query.ms_conc_sum(1, 2)");

        float baseline = compiled.evaluate(new MolangScope()).asFloat();
        assertEquals(13.0f, baseline);

        int threads = 8;
        int iterations = 2_000;
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
                    MolangScope scope = new MolangScope();
                    for (int i = 0; i < iterations; i++) {
                        if (compiled.evaluate(scope).asFloat() != baseline) {
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
        assertTrue(!mismatch.get(), "并发 resolve 出现与串行基线不一致的结果");
    }

    @Test
    void concurrentResolveDuringEpochFlip() throws Exception {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ConcMapping.class)));
        CompiledMolangExpression compiled = compile("query.ms_conc");

        // 基线：注册前 null，注册后 10
        MolangScope probe = new MolangScope();
        int threads = 8;
        int iterations = 2_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean wrongValue = new AtomicBoolean(false);
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
                    MolangScope scope = new MolangScope();
                    for (int i = 0; i < iterations; i++) {
                        float v = compiled.evaluate(scope).asFloat();
                        // 纪元切换窗口内允许旧纪元结果（null→0）或新纪元结果（10），
                        // 但不允许任何第三种值（撕裂迹象）
                        if (v != 0.0f && v != 10.0f) {
                            wrongValue.set(true);
                            return;
                        }
                    }
                });
            }
            start.countDown();
            // 求值进行中翻纪元：重复注册触发 epoch++（addNode 自增纪元）
            for (int k = 0; k < 50; k++) {
                MolangMappingRegistries.mappingTree().addNode("query",
                        new MolangMappingTree.MolangClass(ConcMapping.class, false));
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertTrue(!wrongValue.get(), "纪元翻转窗口出现撕裂值");
        // 收敛：所有线程之后必须看到新纪元结果
        assertEquals(10.0f, compiled.evaluate(probe).asFloat());
    }

    private static CompiledMolangExpression compile(String expr) {
        return new MolangCompilerImpl().compile(expr, CompileContext.defaults());
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class ConcMapping {
        @MolangFunction("ms_conc")
        public static float value() {
            return 10.0f;
        }

        @MolangFunction("ms_conc_sum")
        public static float sum(float a, float b) {
            return a + b;
        }
    }
}
