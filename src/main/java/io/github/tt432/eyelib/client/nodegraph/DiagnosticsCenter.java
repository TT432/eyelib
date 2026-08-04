package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点图诊断中心（规格 nodegraph-declaration-wiring §4.1）：
 * 导入/构建/验证的批量诊断的统一汇聚点，替代聊天栏上报。
 *
 * <p>语义：只保留最新一批（IDEA Problems 窗口语义——面板展示当前问题，不留历史）；
 * 空诊断批次不上报（面板保留上一批）；slf4j 日志始终全量。
 * 编辑器浮动面板经 {@link #addListener} 订阅刷新。
 */
public final class DiagnosticsCenter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCenter.class);

    private DiagnosticsCenter() {
    }

    /** 一节诊断（按文档/库分组，如闭包导入时每 RC 一节）。 */
    public record Section(String label, List<Diagnostic> diagnostics) {
        public Section {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /** 一批诊断（一次导入/构建/打开）。 */
    public record Batch(long epochMillis, String source, List<Section> sections) {
        public Batch {
            sections = List.copyOf(sections);
        }

        public long errors() {
            return sections.stream().flatMap(s -> s.diagnostics().stream())
                    .filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
        }

        public long warnings() {
            return sections.stream().flatMap(s -> s.diagnostics().stream())
                    .filter(d -> d.severity() == Diagnostic.Severity.WARNING).count();
        }
    }

    private static volatile @Nullable Batch latest;
    private static final CopyOnWriteArrayList<Consumer<Batch>> LISTENERS = new CopyOnWriteArrayList<>();

    /** 单节上报（label 通常为库键或文档名）。空诊断忽略。 */
    public static void report(String source, String label, List<Diagnostic> diagnostics) {
        report(source, List.of(new Section(label, diagnostics)));
    }

    /** 多节上报（闭包导入：实体 + 各 RC/AC 各一节）。空诊断忽略。 */
    public static void report(String source, List<Section> sections) {
        long count = sections.stream().mapToLong(s -> s.diagnostics().size()).sum();
        if (count == 0) {
            return;
        }
        Batch batch = new Batch(System.currentTimeMillis(), source, sections);
        latest = batch;
        LOGGER.info("[nodegraph] {}: {} error(s), {} warning(s)", source, batch.errors(), batch.warnings());
        for (Section section : batch.sections()) {
            for (Diagnostic d : section.diagnostics()) {
                if (d.severity() == Diagnostic.Severity.ERROR) {
                    LOGGER.error("[nodegraph] [{}] {} {}", section.label(), d.code(), d.message());
                } else {
                    LOGGER.warn("[nodegraph] [{}] {} {}", section.label(), d.code(), d.message());
                }
            }
        }
        for (Consumer<Batch> listener : LISTENERS) {
            listener.accept(batch);
        }
    }

    public static @Nullable Batch latest() {
        return latest;
    }

    public static void addListener(Consumer<Batch> listener) {
        LISTENERS.addIfAbsent(listener);
    }

    public static void removeListener(Consumer<Batch> listener) {
        LISTENERS.remove(listener);
    }
}
