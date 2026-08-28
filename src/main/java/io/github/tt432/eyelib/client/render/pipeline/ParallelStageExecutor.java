package io.github.tt432.eyelib.client.render.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * 逐实体阶段并行执行器（Opt19）。
 * <p>
 * 线程模型（docs/research/2026-08-26-render-gpu-offload.md 路线 P-Par）：
 * <ul>
 *   <li>渲染线程调用 {@link #forEachEntity}：实体列表按连续索引分片投入专用
 *       ForkJoinPool，渲染线程 join 阻塞——submit/join 建立 happens-before，
 *       worker 对逐实体私有状态（RenderData/scope/组件）的写在 join 后对渲染线程可见。</li>
 *   <li>结果与延迟动作按实体索引序合并/回放，与串行逐实体迭代完全一致。</li>
 *   <li>worker 抛出的首个异常在 join 后于渲染线程重抛（保持现有诊断语义）。</li>
 * </ul>
 * 开关：{@code -Deyelib.parallelStages=false} 整体禁用；{@code -Deyelib.parallelStages.threads=N}
 * 覆盖 worker 数；{@code -Deyelib.parallelStages.minEntities=N} 覆盖并行阈值（低于则串行）。
 *
 * @author TT432
 */
public final class ParallelStageExecutor {
    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.parallelStages", "true"));
    private static final int MIN_ENTITIES =
            Integer.parseInt(System.getProperty("eyelib.parallelStages.minEntities", "8"));
    private static final int THREADS = clamp(
            Integer.parseInt(System.getProperty("eyelib.parallelStages.threads", "0")));

    private static int clamp(int configured) {
        if (configured > 0) {
            return configured;
        }
        // 渲染相内服务器 tick 等其他线程仍在跑，不抢占全部核
        return Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static volatile ForkJoinPool pool;

    // 命名序号独立计数：w.getPoolIndex() 在 setName 时机下恒 0（实证：JFR 里 8 个线程
    // 全部显示 worker-0，靠 javaThreadId 才能区分），会导致诊断/采样聚合误导。
    private static final java.util.concurrent.atomic.AtomicInteger WORKER_NAME_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    private static ForkJoinPool pool() {
        ForkJoinPool p = pool;
        if (p == null) {
            synchronized (ParallelStageExecutor.class) {
                p = pool;
                if (p == null) {
                    ForkJoinPool.ForkJoinWorkerThreadFactory factory = tp -> {
                        ForkJoinWorkerThread w = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(tp);
                        w.setName("eyelib-render-worker-" + WORKER_NAME_SEQ.getAndIncrement());
                        w.setDaemon(true);
                        return w;
                    };
                    p = new ForkJoinPool(THREADS, factory, null, false);
                    pool = p;
                }
            }
        }
        return p;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static int threads() {
        return THREADS;
    }

    /**
     * 对 entities 逐实体执行 work，按索引序合并结果与延迟动作。
     *
     * @param work   worker 线程执行：实体 → 结果；{@code deferred} 收集须渲染线程执行的动作
     *               （GPU 操作、粒子回放等），返回 {@code null} 表示该实体无结果（跳过合并）。
     * @param merger 渲染线程、索引序：消费结果与该实体的延迟动作（deferred 不可变视图，勿持有）。
     */
    public static <E, R> void forEachEntity(List<E> entities,
                                            BiFunction<E, List<Runnable>, R> work,
                                            EntityMerger<R> merger) {
        int n = entities.size();
        if (!ENABLED || n < MIN_ENTITIES) {
            for (E entity : entities) {
                List<Runnable> deferred = new ArrayList<>();
                R result = work.apply(entity, deferred);
                merger.merge(result, deferred);
            }
            return;
        }

        @SuppressWarnings("unchecked")
        R[] results = (R[]) new Object[n];
        @SuppressWarnings("unchecked")
        List<Runnable>[] deferreds = (List<Runnable>[]) new List<?>[n];
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        int partitions = Math.min(n, THREADS * 2);
        pool().submit(() -> {
            try (var stream = java.util.stream.IntStream.range(0, partitions).parallel()) {
                stream.forEach(partition -> {
                    int from = partition * n / partitions;
                    int to = (partition + 1) * n / partitions;
                    for (int i = from; i < to; i++) {
                        List<Runnable> deferred = new ArrayList<>();
                        deferreds[i] = deferred;
                        try {
                            results[i] = work.apply(entities.get(i), deferred);
                        } catch (Throwable t) {
                            firstError.compareAndSet(null, t);
                        }
                    }
                });
            }
        }).join();

        Throwable error = firstError.get();
        if (error != null) {
            if (error instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(error);
        }
        for (int i = 0; i < n; i++) {
            merger.merge(results[i], deferreds[i]);
        }
    }

    @FunctionalInterface
    public interface EntityMerger<R> {
        /** result 为 null 时跳过（实体被 work 判定无需处理）。 */
        void merge(R result, List<Runnable> deferred);
    }

    private ParallelStageExecutor() {
    }
}
