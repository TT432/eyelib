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
    /** 同帧同姿态重复提交被丢弃的次数（26.1 render state 累积 / CarryOn 重复提交的度量）。 */
    private static volatile int duplicateSuppressedCount = 0;
    /** 「画了挂载物（盔甲/手持物）但本体一帧内没有提交几何」的帧数。 */
    private static volatile int attachmentWithoutBodyCount = 0;

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

    /** 记一次「同帧同姿态重复提交」被抑制（26.1 的 render state 累积会重复提交同一实体）。 */
    static void recordDuplicateSuppressed() {
        duplicateSuppressedCount++;
    }

    /**
     * 记一次「挂载物画了、本体没提交」：该帧本体由原版层补画，屏幕上是「本体 + 多一份甲」。
     * 正常为 0；非 0 说明挂载物应先确认本体一定会提交。
     */
    static void recordAttachmentWithoutBody() {
        attachmentWithoutBodyCount++;
    }

    /** 同帧同姿态重复提交被抑制的累计次数，dev 诊断用。 */
    static int getDuplicateSuppressedCount() {
        return duplicateSuppressedCount;
    }

    /** 「画了挂载物但本体未提交」的累计帧数，dev 诊断用。 */
    static int getAttachmentWithoutBodyCount() {
        return attachmentWithoutBodyCount;
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
        duplicateSuppressedCount = 0;
        attachmentWithoutBodyCount = 0;
    }
}
