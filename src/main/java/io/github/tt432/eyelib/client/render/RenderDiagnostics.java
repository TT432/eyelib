package io.github.tt432.eyelib.client.render;

import org.jspecify.annotations.Nullable;

/**
 * 渲染计数/失败记录（dev 诊断），从 {@link EntityRenderOrchestrator} 提取。
 * 由 benchmark/smoke 经 {@link EntityRenderOrchestrator} 的公开 getter 消费。
 *
 * @author TT432
 */
final class RenderDiagnostics {
    private static volatile int renderCount = 0;
    private static volatile int errorCount = 0;
    private static volatile @Nullable String lastError = null;

    private RenderDiagnostics() {
    }

    /** 记一次成功的 Eyelib 实体渲染。 */
    static void recordSuccess() {
        renderCount++;
    }

    /** 记录首个渲染失败供 dev 诊断；异常继续上抛。 */
    static void recordError(Throwable t) {
        errorCount++;
        if (errorCount <= 1) {
            var sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            lastError = sw.toString();
        }
    }

    /** Cumulative successful Eyelib entity renders, exposed for dev diagnostics. */
    static int getRenderCount() {
        return renderCount;
    }

    /** Cumulative render failures, exposed for dev diagnostics. */
    static int getErrorCount() {
        return errorCount;
    }

    /** First recorded render failure, or {@code null} when no failure occurred. */
    static @Nullable String getLastError() {
        return lastError;
    }

    /** Resets dev-only render diagnostics before an isolated measurement. */
    static void reset() {
        renderCount = 0;
        errorCount = 0;
        lastError = null;
    }
}
