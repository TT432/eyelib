package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import java.util.List;

/**
 * JSON → 图 导入结果。
 *
 * @param library     产出的图库（未注册、未落盘；即使含 ERROR 诊断也尽力产出）
 * @param diagnostics 导入诊断（不支持的结构 / 未知字段 / 解析失败等，原文经便签保留）
 */
public record ImportResult(GraphLibrary library, List<Diagnostic> diagnostics) {
    public ImportResult {
        diagnostics = List.copyOf(diagnostics);
    }

    /** 诊断列表是否含 ERROR。 */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }
}
