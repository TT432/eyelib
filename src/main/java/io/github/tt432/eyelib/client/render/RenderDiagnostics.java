package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.bridge.EnvironmentPort;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 渲染计数/失败记录（dev 诊断），从 {@link EntityRenderOrchestrator} 提取。
 * 由 benchmark/smoke 经 {@link EntityRenderOrchestrator} 的公开 getter 消费。
 *
 * @author TT432
 */
final class RenderDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderDiagnostics.class);
    private static volatile int renderCount = 0;
    private static volatile int errorCount = 0;
    private static volatile @Nullable String lastError = null;
    /** 同帧同姿态重复提交被丢弃的次数（26.1 render state 累积 / CarryOn 重复提交的度量）。 */
    private static volatile int duplicateSuppressedCount = 0;
    /** 「画了挂载物（盔甲/手持物）但本体一帧内没有提交几何」的帧数。 */
    private static volatile int attachmentWithoutBodyCount = 0;
    /** 同一帧内同 identifier 的挂载声明被丢弃的次数（跨源命名冲突/重复声明）。 */
    private static volatile int attachmentDeclarationDroppedCount = 0;
    /** 挂载声明源在 declare/draw 中抛异常的次数（单源异常被隔离，不影响其它源）。 */
    private static volatile int attachmentSourceErrorCount = 0;
    /** 按 {@link RenderSkipReason} 的跳过计数。 */
    private static final java.util.concurrent.atomic.AtomicIntegerArray SKIP_COUNTS =
            new java.util.concurrent.atomic.AtomicIntegerArray(RenderSkipReason.values().length);
    /** 首个跳过的现场描述（含调用方帧，便于定位「谁漏了宿主」）。 */
    private static volatile @Nullable String lastSkipDetail = null;
    /** 严格度覆盖（测试/诊断用）。 */
    enum Strictness { DEFAULT, STRICT, LENIENT }

    /** 覆盖值；{@code DEFAULT} = 按环境判定。 */
    private static volatile Strictness strictOverride = Strictness.DEFAULT;

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

    /**
     * 记录一次渲染跳过。首个跳过会就地抓取调用栈——「没有宿主也没有日志」正是本类要消灭的缺陷，
     * 而调用方帧是唯一能直接指向漏传宿主的调用点的信息。
     */
    static void recordSkip(RenderSkipReason reason, String detail) {
        SKIP_COUNTS.incrementAndGet(reason.ordinal());
        if (lastSkipDetail != null) {
            return;
        }
        var stack = Thread.currentThread().getStackTrace();
        String caller = "unknown";
        for (var frame : stack) {
            String cls = frame.getClassName();
            if (!cls.startsWith("java.") && !cls.equals(RenderDiagnostics.class.getName())) {
                caller = cls + "." + frame.getMethodName() + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
                break;
            }
        }
        String message = reason + ": " + detail + " @ " + caller;
        lastSkipDetail = message;
        // 生产环境只提示一次，避免刷屏；严格模式（dev）由调用方抛错，不需要日志
        if (!isStrict()) {
            LOGGER.warn("[Eyelib] 模型渲染跳过 —— {}", message);
        }
    }

    /** 指定跳过原因的累计次数。 */
    static int getSkipCount(RenderSkipReason reason) {
        return SKIP_COUNTS.get(reason.ordinal());
    }

    /** 首个跳过的现场描述，{@code null} 表示本次会话从未跳过。 */
    static @Nullable String getLastSkipDetail() {
        return lastSkipDetail;
    }

    /**
     * 是否对渲染跳过采取 fail-fast。
     * <p>
     * 只在可确定为**开发环境**时抛错：26.1 无法判定（{@code ForgeEnvironment} 在该节点只能返回
     * UNKNOWN），此时退化为计数 + 一次 WARN，避免在玩家侧崩溃。
     */
    static boolean isStrict() {
        return switch (strictOverride) {
            case STRICT -> true;
            case LENIENT -> false;
            case DEFAULT -> EnvironmentPort.isDevelopment();
        };
    }

    /** 测试用：覆盖严格度判定（{@code DEFAULT} 恢复按环境判定）。 */
    static void setStrictOverrideForTest(Strictness strictness) {
        strictOverride = strictness;
    }

    /** 挂载声明同帧同 identifier 被丢弃的累计次数，dev 诊断用。 */
    static int getAttachmentDeclarationDroppedCount() {
        return attachmentDeclarationDroppedCount;
    }

    /** 挂载声明源抛异常的累计次数，dev 诊断用。 */
    static int getAttachmentSourceErrorCount() {
        return attachmentSourceErrorCount;
    }

    static void recordAttachmentDeclarationDropped() {
        attachmentDeclarationDroppedCount++;
    }

    static void recordAttachmentSourceError() {
        attachmentSourceErrorCount++;
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
        attachmentDeclarationDroppedCount = 0;
        attachmentSourceErrorCount = 0;
        lastSkipDetail = null;
        for (int i = 0; i < SKIP_COUNTS.length(); i++) {
            SKIP_COUNTS.set(i, 0);
        }
    }
}
