package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import java.util.List;

/**
 * 代码生成结果：Molang ExprSet 字符串 + 诊断列表。
 *
 * <p>即使存在 ERROR 诊断也会产出占位输出（未连接输入以 {@code 0} 占位），
 * 调用方应以 {@link #hasErrors()} 决定是否采用产物。
 *
 * @param code        生成的 Molang ExprSet 字符串
 * @param diagnostics 生成过程诊断（未连接输入、子图递归等）
 */
public record CodegenResult(String code, List<Diagnostic> diagnostics) {

    public CodegenResult {
        diagnostics = List.copyOf(diagnostics);
    }

    /** 是否含 ERROR 级诊断。 */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }
}
