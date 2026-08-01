package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import java.util.List;

/**
 * 组装结果：Bedrock 文档 JSON（Gson 树）+ 诊断列表。
 *
 * <p>即使存在 ERROR 诊断也会尽力产出 JSON，调用方应以 {@link #hasErrors()} 决定是否采用产物。
 *
 * @param json        组装出的 Bedrock 文档
 * @param diagnostics 组装诊断（自身诊断 + 透传的代码生成诊断）
 */
public record AssemblyResult(JsonObject json, List<Diagnostic> diagnostics) {

    public AssemblyResult {
        diagnostics = List.copyOf(diagnostics);
    }

    /** 是否含 ERROR 级诊断。 */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }
}
