package io.github.tt432.eyelib.snowstorm.editor;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Snowstorm v3.2.2 {@code edits.js} 逐字移植（原作者 JannisX11，GPL-3.0）：
 * typing 编辑 600ms 合并后通知 {@link #EditListeners}。
 *
 * <p>偏离记录：
 * <ul>
 *   <li>VSCode 消息通道剔除（ADR-0036 D6）：{@code processEdit} 中
 *       {@code if (vscode) { compileJSON(generateFile()) ... }} 分支不移植
 *       （generateFile 仅服务该分支），processEdit 只剩监听器派发。</li>
 *   <li>JS 的 {@code registerEdit(id, event, cooldown)} 以
 *       {@code event instanceof InputEvent || event instanceof KeyboardEvent} 判断 typing；
 *       浏览器事件类型不存在，Java 侧收缩为 boolean 参数
 *       {@code typingOrCooldown}（语义 = typing 事件 || cooldown）。</li>
 *   <li>window 'request_content_update' 消息监听不移植（VSCode 专用）；
 *       其 flush 语义由 {@link #flush()}（原 wrapTimeoutInit）提供。</li>
 *   <li>JS setTimeout/clearTimeout → 单线程 daemon {@link ScheduledExecutorService}；
 *       时间基准注入 {@link LongSupplier} clock（默认 {@code System::currentTimeMillis}），
 *       合并截止时刻以 clock 记录，oracle 测试可注入确定性时钟断言合并行为。</li>
 * </ul>
 */
public final class EditHistory {

    private EditHistory() {
    }

    /** JS {@code EditListeners}：键任意字符串，handler 接收 edit id。 */
    public static final Map<String, Consumer<String>> EditListeners = new LinkedHashMap<>();

    private static final Object LOCK = new Object();

    private static int typing_merge_threshold = 600;
    private static String last_edit_id = "";
    private static LongSupplier clock = System::currentTimeMillis;

    private static @Nullable ScheduledExecutorService scheduler;
    private static @Nullable ScheduledFuture<?> timeout;
    /** 当前待派发合并编辑的截止时刻（clock 基准，毫秒）；无待派发时为 0。 */
    private static long deadline;

    private static ScheduledExecutorService scheduler() {
        synchronized (LOCK) {
            ScheduledExecutorService current = scheduler;
            if (current == null) {
                current = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread thread = new Thread(r, "snowstorm-edit-history");
                    thread.setDaemon(true);
                    return thread;
                });
                scheduler = current;
            }
            return current;
        }
    }

    @SuppressWarnings("unchecked")
    private static void processEdit(String id) {
        Consumer<String>[] handlers;
        synchronized (LOCK) {
            handlers = EditListeners.values().toArray(new Consumer[0]);
        }
        for (Consumer<String> handler : handlers) {
            handler.accept(id);
        }
    }

    /** JS {@code registerEdit(id)}：非 typing 事件，立即派发。 */
    public static void registerEdit(String id) {
        synchronized (LOCK) {
            last_edit_id = id;
        }
        processEdit(id);
    }

    /**
     * JS {@code registerEdit(id, event, cooldown)}：
     * {@code typingOrCooldown} 为 true（typing/键盘事件或显式 cooldown）时 600ms 合并，
     * 否则立即派发。
     */
    public static void registerEdit(String id, boolean typingOrCooldown) {
        if (typingOrCooldown) {
            synchronized (LOCK) {
                last_edit_id = id;
                if (timeout != null) timeout.cancel(false);
                long now = clock.getAsLong();
                deadline = now + typing_merge_threshold;
                long delay = Math.max(deadline - clock.getAsLong(), 0);
                timeout = scheduler().schedule(() -> {
                    synchronized (LOCK) {
                        timeout = null;
                        deadline = 0;
                    }
                    processEdit(id);
                }, delay, TimeUnit.MILLISECONDS);
            }
        } else {
            registerEdit(id);
        }
    }

    /**
     * 原 wrapTimeoutInit（window 'request_content_update' 处理器）：
     * 有待派发的合并编辑时取消计时并立即派发。
     */
    public static void flush() {
        String idToProcess = null;
        synchronized (LOCK) {
            if (timeout != null) {
                timeout.cancel(false);
                timeout = null;
                deadline = 0;
                idToProcess = last_edit_id;
            }
        }
        if (idToProcess != null) {
            processEdit(idToProcess);
        }
    }

    /** 当前是否有待派发的合并编辑。 */
    public static boolean hasPendingEdit() {
        synchronized (LOCK) {
            return timeout != null;
        }
    }

    /** 待派发合并编辑的截止时刻（clock 基准毫秒），无待派发为 0。 */
    public static long pendingDeadline() {
        synchronized (LOCK) {
            return deadline;
        }
    }

    /** 注入时钟（oracle 测试用；须在测试结束后 {@link #resetClock()}）。 */
    public static void setClock(LongSupplier newClock) {
        synchronized (LOCK) {
            clock = newClock;
        }
    }

    /** 恢复默认时钟。 */
    public static void resetClock() {
        synchronized (LOCK) {
            clock = System::currentTimeMillis;
        }
    }

    /** 调整合并阈值（毫秒，as-is 默认 600；oracle 测试用）。 */
    public static void setTypingMergeThreshold(int thresholdMillis) {
        synchronized (LOCK) {
            typing_merge_threshold = thresholdMillis;
        }
    }
}
