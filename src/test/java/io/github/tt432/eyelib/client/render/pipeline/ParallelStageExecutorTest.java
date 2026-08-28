package io.github.tt432.eyelib.client.render.pipeline;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ParallelStageExecutor 契约（Opt19）：
 * <ul>
 *     <li>分片覆盖——每个元素恰好处理一次；</li>
 *     <li>合并顺序 == 索引顺序（并行与串行一致）；</li>
 *     <li>worker 异常在渲染线程重抛；</li>
 *     <li>null 结果由合并方跳过，延迟动作按序回放。</li>
 * </ul>
 *
 * @author TT432
 */
class ParallelStageExecutorTest {

    @Test
    void parallelCoversEveryIndexExactlyOnceAndMergesInOrder() {
        int n = 1_000;
        List<Integer> entities = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            entities.add(i);
        }
        AtomicInteger processed = new AtomicInteger();
        List<Integer> merged = new ArrayList<>();
        List<String> deferredLog = new ArrayList<>();

        ParallelStageExecutor.forEachEntity(entities,
                (entity, deferred) -> {
                    processed.incrementAndGet();
                    deferred.add(() -> deferredLog.add("d" + entity));
                    return entity * 10;
                },
                (result, deferred) -> {
                    merged.add(result);
                    deferred.forEach(Runnable::run);
                });

        assertEquals(n, processed.get(), "每个元素必须恰好处理一次");
        assertEquals(n, merged.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i * 10, merged.get(i), "合并顺序必须等于索引顺序");
            assertEquals("d" + i, deferredLog.get(i), "延迟动作必须按索引序回放");
        }
    }

    @Test
    void nullResultsAreSkippableByMerger() {
        List<Integer> entities = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> merged = new ArrayList<>();
        ParallelStageExecutor.forEachEntity(entities,
                (entity, deferred) -> entity % 2 == 0 ? entity : null,
                (result, deferred) -> {
                    if (result != null) {
                        merged.add(result);
                    }
                });
        assertEquals(List.of(0, 2, 4, 6, 8), merged);
    }

    @Test
    void workerExceptionRethrownOnCallerThread() {
        int n = 64;
        List<Integer> entities = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            entities.add(i);
        }
        IllegalStateException boom = assertThrows(IllegalStateException.class, () ->
                ParallelStageExecutor.forEachEntity(entities,
                        (entity, deferred) -> {
                            if (entity == 42) {
                                throw new IllegalStateException("boom");
                            }
                            return entity;
                        },
                        (result, deferred) -> {
                        }));
        assertEquals("boom", boom.getMessage());
    }

    @Test
    void emptyListIsNoOp() {
        AtomicInteger processed = new AtomicInteger();
        ParallelStageExecutor.forEachEntity(List.of(),
                (Object entity, List<Runnable> deferred) -> {
                    processed.incrementAndGet();
                    return entity;
                },
                (result, deferred) -> {
                });
        assertEquals(0, processed.get());
    }

    @Test
    void smallListRunsSerialWithSameSemantics() {
        // 低于 minEntities 阈值（默认 8）→ 串行路径；结果必须等价
        List<Integer> entities = List.of(1, 2, 3);
        List<Integer> merged = new ArrayList<>();
        List<String> deferredLog = new ArrayList<>();
        ParallelStageExecutor.forEachEntity(entities,
                (entity, deferred) -> {
                    deferred.add(() -> deferredLog.add("d" + entity));
                    return entity + 100;
                },
                (result, deferred) -> {
                    merged.add(result);
                    deferred.forEach(Runnable::run);
                });
        assertEquals(List.of(101, 102, 103), merged);
        assertEquals(List.of("d1", "d2", "d3"), deferredLog);
        assertTrue(true, "串行路径语义与并行一致由上方断言覆盖");
    }
}
